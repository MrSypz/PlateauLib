package com.sypztep.plateau.common.v1.network;

import com.sypztep.plateau.PlateauSyncConfig;
import com.sypztep.plateau.common.v1.network.payload.SyncAckC2S;
import com.sypztep.plateau.common.v1.network.payload.SyncDataS2C;
import com.sypztep.plateau.common.v1.network.payload.SyncHelloS2C;
import com.sypztep.plateau.common.v1.network.payload.SyncResponseC2S;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Server-side state machine for the universal config sync handshake.
 *
 * <p>Manages one {@link SyncState} per connected player UUID. Players are
 * placed in {@link GameType#SPECTATOR spectator mode} on join and released only
 * after the handshake completes — either via the fast path (cache hit) or the
 * slow path (data exchange).
 *
 * <hr>
 * <h3>Fast path — client has all caches valid:</h3>
 * <pre>
 *   JOIN  →  freeze(spectator)  →  SyncHelloS2C(hashes)
 *         →  SyncResponseC2S(missing=[], masterHash)
 *         →  verify masterHash  →  unfreeze
 * </pre>
 *
 * <h3>Slow path — client is missing one or more namespaces:</h3>
 * <pre>
 *   JOIN  →  freeze(spectator)  →  SyncHelloS2C(hashes)
 *         →  SyncResponseC2S(missing=[ns1, ns2, ...])
 *         →  SyncDataS2C(data, masterHash)
 *         →  SyncAckC2S(masterHash)
 *         →  verify masterHash  →  unfreeze
 * </pre>
 *
 * <h3>Stale fast-path fallback:</h3>
 * <p>If the client sends a fast-path {@code SyncResponseC2S} but its computed
 * master hash does not match the server's, the server silently upgrades the
 * session to the slow path and sends fresh data for <em>all</em> namespaces
 * rather than disconnecting the player.
 *
 * <h3>Timeout safety net:</h3>
 * <p>A {@value #TIMEOUT_SECONDS}-second watchdog timer is scheduled on join.
 * If the handshake has not completed by then (buggy client, network issue),
 * the player is released back to their real game mode with a warning log.
 * This prevents permanent spectator lock.
 *
 * <h3>Disconnect cleanup:</h3>
 * <p>If a player disconnects mid-handshake, {@link #onPlayerDisconnect} removes
 * their entry from {@link #pendingPlayers} to prevent memory leaks. The timeout
 * scheduler checks for the entry before acting, so a late-firing timeout for a
 * disconnected player is a safe no-op.
 */
public final class ConfigSyncManager {

    private ConfigSyncManager() {}

    /** Seconds to wait for a client to complete the handshake before releasing them. */
    private static final int TIMEOUT_SECONDS = 10;

    /**
     * The two server-side states a player can be in during the handshake.
     */
    public enum SyncState {
        /** Hello sent. Waiting for {@link SyncResponseC2S} (fast or slow path). */
        AWAITING_RESPONSE,
        /** Data sent. Waiting for {@link SyncAckC2S} to confirm the client applied it. */
        AWAITING_ACK
    }

    /**
     * Holds the handshake state for one player.
     *
     * @param realGameMode  the {@link GameType} to restore when the handshake completes
     * @param state         the current stage of the handshake
     */
    private record PlayerSyncEntry(GameType realGameMode, SyncState state) {}

    /**
     * Active handshakes, keyed by player UUID.
     *
     * <p>Uses {@link ConcurrentHashMap} because JOIN events and packet handler callbacks
     * can arrive on different threads (Netty vs. server thread).
     */
    private static final Map<UUID, PlayerSyncEntry> pendingPlayers = new ConcurrentHashMap<>();

    /**
     * Single-thread scheduler for timeout watchdogs.
     * Daemon thread so it does not prevent JVM shutdown.
     */
    private static final ScheduledExecutorService TIMEOUT_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "plateau-sync-config-timeout");
                thread.setDaemon(true);
                return thread;
            });

    // -------------------------------------------------------------------------
    // Lifecycle hooks — called from ServerPlayConnectionEvents
    // -------------------------------------------------------------------------

    /**
     * Initiates the config sync handshake for a newly joined player.
     *
     * <p>Saves the player's current game mode, switches them to spectator,
     * sends {@link SyncHelloS2C}, and schedules the timeout watchdog.
     *
     * <p>Called from {@link net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents#JOIN JOIN}.
     * Only active on dedicated servers — integrated/LAN servers skip the handshake.
     *
     * @param player the player who just joined
     * @param server the running {@link MinecraftServer} instance (used to schedule the timeout on the server thread)
     */
    public static void onPlayerJoin(ServerPlayer player, MinecraftServer server) {
        if (ConfigSyncRegistry.namespaces().isEmpty()) {
            PlateauSyncConfig.LOGGER.info(
                    "No configs registered — skipping handshake for {}",
                    player.getName().getString());
            return;
        }

        if (!ServerPlayNetworking.canSend(player, SyncHelloS2C.ID)) {
            PlateauSyncConfig.LOGGER.info(
                    "{} does not have the sync lib — skipping handshake",
                    player.getName().getString());
            return;
        }

        GameType realGameMode = player.gameMode.getGameModeForPlayer();
        pendingPlayers.put(player.getUUID(), new PlayerSyncEntry(realGameMode, SyncState.AWAITING_RESPONSE));
        player.setGameMode(GameType.SPECTATOR);

        // Collect hashes once — reused for both the log message and the packet
        Map<String, Integer> currentHashes = ConfigSyncRegistry.collectHashes();
        int masterHash = ConfigSyncRegistry.masterHash(currentHashes);

        PlateauSyncConfig.LOGGER.info(
                "Sync started for {} (namespaces={}, masterHash={})",
                player.getName().getString(), ConfigSyncRegistry.namespaces(), masterHash);

        SyncHelloS2C.send(player, currentHashes);
        scheduleTimeoutWatchdog(server, player);
    }

    /**
     * Cleans up the handshake state when a player disconnects.
     *
     * <p>Safe to call even if the player was not mid-handshake (e.g. disconnected
     * after the handshake completed). The {@link ConcurrentHashMap#remove} is a no-op
     * in that case.
     *
     * <p>Called from {@link net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents#DISCONNECT DISCONNECT}.
     *
     * @param player the player who disconnected
     */
    public static void onPlayerDisconnect(ServerPlayer player) {
        pendingPlayers.remove(player.getUUID());
    }

    // -------------------------------------------------------------------------
    // Packet handlers — called from ServerPlayNetworking receivers
    // -------------------------------------------------------------------------

    /**
     * Handles {@link SyncResponseC2S} — the client's reply to {@link SyncHelloS2C}.
     *
     * <p>Two cases:
     * <ul>
     *   <li><b>Fast path</b> ({@link SyncResponseC2S#isFastPath() isFastPath()} is {@code true}):
     *       The client claims all its caches are valid and sends its computed master hash.
     *       If the hash matches the server's, the player is unfrozen immediately.
     *       If it mismatches (stale cache), the server falls through to the slow path.</li>
     *   <li><b>Slow path</b>: The client lists the namespaces it is missing.
     *       The server serializes only those namespaces and sends {@link SyncDataS2C}.</li>
     * </ul>
     *
     * @param player the player whose client sent this packet
     * @param packet the received {@link SyncResponseC2S}
     */
    public static void onSyncResponse(ServerPlayer player, SyncResponseC2S packet) {
        PlayerSyncEntry entry = pendingPlayers.get(player.getUUID());
        if (entry == null || entry.state() != SyncState.AWAITING_RESPONSE) return;

        if (packet.isFastPath()) {
            int expectedMasterHash = ConfigSyncRegistry.masterHash();

            if (packet.masterHash() == expectedMasterHash) {
                // ── Fast path success: hashes agree, nothing to send ─────────
                unfreeze(player, entry.realGameMode());
            } else {
                // ── Stale fast path: hash mismatch, upgrade to slow path ─────
                PlateauSyncConfig.LOGGER.info(
                        "Fast-path hash mismatch for {} (clientHash={} serverHash={}) "
                        + "— sending full config",
                        player.getName().getString(), packet.masterHash(), expectedMasterHash);
                pendingPlayers.put(player.getUUID(), new PlayerSyncEntry(entry.realGameMode(), SyncState.AWAITING_ACK));
                SyncDataS2C.send(player, new ArrayList<>(ConfigSyncRegistry.namespaces()));
            }

        } else {
            // ── Slow path: send only the namespaces the client asked for ─────
            PlateauSyncConfig.LOGGER.info(
                    "Sending data for [{}] to {}",
                    String.join(", ", packet.missing()), player.getName().getString());
            pendingPlayers.put(player.getUUID(), new PlayerSyncEntry(entry.realGameMode(), SyncState.AWAITING_ACK));
            SyncDataS2C.send(player, packet.missing());
        }
    }

    /**
     * Handles {@link SyncAckC2S} — the client's confirmation that it applied the data.
     *
     * <p>Verifies the echoed master hash against the server's current value.
     * On success, the player is unfrozen. On mismatch, the player is kicked with an
     * instructive message — this typically means a network corruption issue.
     *
     * @param player the player whose client sent this packet
     * @param packet the received {@link SyncAckC2S}
     */
    public static void onSyncAck(ServerPlayer player, SyncAckC2S packet) {
        PlayerSyncEntry entry = pendingPlayers.get(player.getUUID());
        if (entry == null || entry.state() != SyncState.AWAITING_ACK) return;

        int expectedMasterHash = ConfigSyncRegistry.masterHash();
        if (packet.masterHash() != expectedMasterHash) {
            PlateauSyncConfig.LOGGER.warn(
                    "Ack hash mismatch from {} (clientHash={} serverHash={}) — kicking",
                    player.getName().getString(), packet.masterHash(), expectedMasterHash);
            pendingPlayers.remove(player.getUUID());
            player.connection.disconnect(Component.literal(
                    "Config sync failed — please rejoin. "
                    + "If this repeats, report it to the server admin."));
            return;
        }

        unfreeze(player, entry.realGameMode());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Removes the player from {@link #pendingPlayers} and restores their game mode.
     *
     * @param player       the player to release
     * @param realGameMode the {@link GameType} they had before the handshake freeze
     */
    private static void unfreeze(ServerPlayer player, GameType realGameMode) {
        pendingPlayers.remove(player.getUUID());
        player.setGameMode(realGameMode);
        PlateauSyncConfig.LOGGER.info(
                "Sync complete for {} — restored {}",
                player.getName().getString(), realGameMode.getName());
    }

    /**
     * Schedules a timeout watchdog that releases a player if the handshake has not
     * completed within {@value #TIMEOUT_SECONDS} seconds.
     *
     * <p>The timeout callback executes on the server thread via {@link MinecraftServer#execute}.
     * It checks whether the UUID is still in {@link #pendingPlayers} before acting, so a late
     * fire for a player who already completed or disconnected is a safe no-op.
     *
     * @param server the server (used to schedule work on its thread)
     * @param player the player being watched
     */
    private static void scheduleTimeoutWatchdog(MinecraftServer server, ServerPlayer player) {
        UUID playerUuid = player.getUUID();
        TIMEOUT_EXECUTOR.schedule(
                () -> server.execute(() -> {
                    PlayerSyncEntry timedOutEntry = pendingPlayers.get(playerUuid);
                    if (timedOutEntry != null) {
                        PlateauSyncConfig.LOGGER.warn(
                                "Handshake timeout for {} after {}s — releasing from spectator",
                                player.getName().getString(), TIMEOUT_SECONDS);
                        unfreeze(player, timedOutEntry.realGameMode());
                    }
                }),
                TIMEOUT_SECONDS, TimeUnit.SECONDS
        );
    }
}
