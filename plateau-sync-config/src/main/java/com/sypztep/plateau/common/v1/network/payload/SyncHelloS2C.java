package com.sypztep.plateau.common.v1.network.payload;

import com.sypztep.plateau.PlateauSyncConfig;
import com.sypztep.plateau.common.v1.config.ServerConfigCache;
import com.sypztep.plateau.common.v1.network.ConfigSyncRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;

import java.util.*;

/**
 * <b>Server → Client.</b> First packet in the config sync handshake.
 *
 * <p>Carries the current hash for every registered namespace so the client
 * can determine which configs it already has cached (valid hash) and which
 * it needs to request (missing or stale hash).
 *
 * <p>The client replies with {@link SyncResponseC2S}:
 * <ul>
 *   <li><b>Fast path</b> — all hashes matched the local cache; client sends back
 *       its computed master hash for server verification.</li>
 *   <li><b>Slow path</b> — one or more hashes were unknown or stale; client
 *       sends the list of namespace keys it needs data for.</li>
 * </ul>
 *
 * @param hashes a map of {@code namespace → current config hash}, one entry per registered config
 */
public record SyncHelloS2C(Map<String, Integer> hashes) implements CustomPacketPayload {
    public static final Type<SyncHelloS2C> ID = new Type<>(PlateauSyncConfig.id("sync_hello"));
    /**
     * Format: [varInt count] [utf namespace, varInt hash] × count
     */
    public static final StreamCodec<FriendlyByteBuf, SyncHelloS2C> CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeVarInt(packet.hashes().size());
                packet.hashes().forEach((namespace, hash) -> {
                    buffer.writeUtf(namespace);
                    buffer.writeVarInt(hash);
                });
            },
            buffer -> {
                int count = buffer.readVarInt();
                Map<String, Integer> hashMap = new LinkedHashMap<>(count);
                for (int i = 0; i < count; i++) hashMap.put(buffer.readUtf(), buffer.readVarInt());
                return new SyncHelloS2C(Collections.unmodifiableMap(hashMap));
            }
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() { return ID; }

    /**
     * Server-side helper. Sends a {@code SyncHelloS2C} to {@code player} using the provided
     * pre-collected hash snapshot. Callers should collect hashes once and reuse the map
     * (e.g. for logging the master hash) rather than calling {@link ConfigSyncRegistry#collectHashes()}
     * twice.
     *
     * @param player         the player to send the packet to
     * @param currentHashes  a snapshot of all registered namespace hashes, from
     *                       {@link ConfigSyncRegistry#collectHashes()}
     */
    public static void send(ServerPlayer player, Map<String, Integer> currentHashes) {
        ServerPlayNetworking.send(player, new SyncHelloS2C(currentHashes));
    }

    // -------------------------------------------------------------------------
    // Client-side receiver
    // -------------------------------------------------------------------------

    /**
     * Handles {@link SyncHelloS2C} on the client.
     *
     * <p>For each namespace in the packet, checks the local {@link ServerConfigCache}.
     * Namespaces with a valid cached entry are applied immediately from cache.
     * Namespaces that are missing or stale are collected into a {@code missing} list.
     *
     * <p>If {@code missing} is empty, replies with {@link SyncResponseC2S#fast} (fast path).
     * Otherwise, replies with {@link SyncResponseC2S#slow} listing which namespaces to send.
     *
     * <p><b>Thread note:</b> The cache validity check runs off the render thread (Netty).
     * To apply + send is wrapped in {@code client.execute()} to run on the render thread.
     */
    public static class Receiver implements ClientPlayNetworking.PlayPayloadHandler<SyncHelloS2C> {

        @Override
        public void receive(SyncHelloS2C packet, ClientPlayNetworking.@NonNull Context ctx) {
            String serverAddress = Objects.requireNonNull(ctx.client().getCurrentServer()).ip;
            List<String> missingNamespaces = new ArrayList<>();
            packet.hashes().forEach((namespace, serverHash) -> {
                if (!ServerConfigCache.isValid(serverAddress, namespace, serverHash))
                    missingNamespaces.add(namespace);
            });

            ctx.client().execute(() -> {
                if (missingNamespaces.isEmpty()) {
                    // ── Fast path: apply all from cache ──────────────────────
                    Map<String, String> cachedData = new LinkedHashMap<>();
                    packet.hashes().forEach((namespace, hash) ->
                            ServerConfigCache.tryLoad(serverAddress, namespace, hash)
                                    .ifPresent(json -> cachedData.put(namespace, json)));

                    ConfigSyncRegistry.applyBatch(cachedData);

                    int clientMasterHash = ConfigSyncRegistry.masterHash(packet.hashes());
                    ClientPlayNetworking.send(SyncResponseC2S.fast(clientMasterHash));

                } else {
                    // ── Slow path: ask server for the missing namespaces ──────
                    ClientPlayNetworking.send(SyncResponseC2S.slow(missingNamespaces));
                }
            });
        }
    }
}
