package com.sypztep.plateau.common.v1.network.payload;

import com.sypztep.plateau.PlateauSyncConfig;
import com.sypztep.plateau.common.v1.config.ServerConfigCache;
import com.sypztep.plateau.common.v1.network.ConfigSyncRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;

import java.util.*;

/**
 * <b>Server → Client.</b> Slow-path data packet. Sent only after receiving a
 * {@link SyncResponseC2S} that lists one or more missing namespaces, or when the server
 * detects a stale master hash in a fast-path {@link SyncResponseC2S}.
 *
 * <p>Contains the serialized config JSON for every requested namespace, plus the
 * server's {@code masterHash} which the client must echo back in {@link SyncAckC2S}
 * to confirm it applied the data correctly.
 *
 * <p>On the client, the receiver:
 * <ol>
 *   <li>Applies all received configs via {@link ConfigSyncRegistry#applyBatch}
 *       (on the render thread).</li>
 *   <li>Writes each namespace to {@link ServerConfigCache} for future joins.</li>
 *   <li>Validates the applied master hash against the server's value (logs a warning
 *       on mismatch but proceeds — the server is the authority).</li>
 *   <li>Sends {@link SyncAckC2S} echoing the server's master hash.</li>
 * </ol>
 *
 * @param receivedData a map of {@code namespace → serialized config JSON}
 * @param masterHash   the server's master hash at the time this packet was built;
 *                     echoed back by the client in {@link SyncAckC2S}
 */
public record SyncDataS2C(Map<String, String> receivedData, int masterHash) implements CustomPacketPayload {
    public static final Type<SyncDataS2C> ID = new Type<>(PlateauSyncConfig.id("sync_data"));

    /**
     * Format: [varInt count] [utf namespace, utf json] × count, [varInt masterHash]
     */
    public static final StreamCodec<FriendlyByteBuf, SyncDataS2C> CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeVarInt(packet.receivedData().size());
                packet.receivedData().forEach((namespace, json) -> {
                    buffer.writeUtf(namespace);
                    buffer.writeUtf(json);
                });
                buffer.writeVarInt(packet.masterHash());
            },
            buffer -> {
                int count = buffer.readVarInt();
                Map<String, String> dataMap = new LinkedHashMap<>(count);
                for (int i = 0; i < count; i++) dataMap.put(buffer.readUtf(), buffer.readUtf());
                int hash = buffer.readVarInt();
                return new SyncDataS2C(Collections.unmodifiableMap(dataMap), hash);
            }
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    /**
     * Server-side helper. Serializes the requested namespaces from the registry and
     * sends the packet to {@code player}.
     *
     * @param player              the player to send the data to
     * @param requestedNamespaces the namespaces to include; typically from
     *                            {@link SyncResponseC2S#missing()} or all registered namespaces
     *                            on a stale fast-path fallback
     */
    public static void send(ServerPlayer player, Collection<String> requestedNamespaces) {
        ServerPlayNetworking.send(player, new SyncDataS2C(
                ConfigSyncRegistry.serializeFor(requestedNamespaces),
                ConfigSyncRegistry.masterHash()
        ));
    }

    // -------------------------------------------------------------------------
    // Client-side receiver
    // -------------------------------------------------------------------------

    /**
     * Handles {@link SyncDataS2C} on the client.
     *
     * <p><b>Must run on the render thread</b> — wrapped in {@code client.execute()} here
     * so consumer mod appliers (registered via
     * {@link ConfigSyncRegistry#register ConfigSyncRegistry.register()})
     * do not need to worry about thread safety.
     */
    public static class Receiver implements ClientPlayNetworking.PlayPayloadHandler<SyncDataS2C> {

        @Override
        public void receive(SyncDataS2C packet, ClientPlayNetworking.@NonNull Context ctx) {
            String serverAddress = Objects.requireNonNull(ctx.client().getCurrentServer()).ip;

            // Everything runs on the render thread — consumer appliers are not thread-safe
            ctx.client().execute(() -> {
                // 1. Apply all received configs through the registry
                ConfigSyncRegistry.applyBatch(packet.receivedData());

                // 2. Persist each namespace to the local cache using the post-apply hash
                Map<String, Integer> appliedHashes = ConfigSyncRegistry.collectHashes();
                packet.receivedData().forEach((namespace, json) -> {
                    int namespaceHash = appliedHashes.getOrDefault(namespace, 0);
                    ServerConfigCache.save(serverAddress, namespace, json, namespaceHash);
                });

                // 3. Validate: applied master hash should match what the server sent
                int appliedMasterHash = ConfigSyncRegistry.masterHash();
                if (appliedMasterHash != packet.masterHash()) {
                    PlateauSyncConfig.LOGGER.warn(
                            "Applied master hash does not match server's "
                                    + "(applied={} server={}) — proceeding anyway; server will verify",
                            appliedMasterHash, packet.masterHash());
                }

                // 4. Echo the server's master hash — server uses this to unfreeze us
                ClientPlayNetworking.send(new SyncAckC2S(packet.masterHash()));
            });
        }
    }
}
