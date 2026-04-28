package com.sypztep.plateau.common.v1.network.payload;

import com.sypztep.plateau.PlateauSyncConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * <b>Client → Server.</b> Reply to {@link SyncHelloS2C}.
 *
 * <p>Encodes one of two mutually exclusive paths:
 *
 * <ul>
 *   <li><b>Fast path</b> — {@link #missing} is empty. The client has valid cached entries
 *       for every namespace and sends its locally-computed {@link #masterHash} for the
 *       server to verify. If the hash matches, the server unfreezes the player immediately
 *       without sending any config data.</li>
 *   <li><b>Slow path</b> — {@link #missing} lists the namespaces whose caches are absent
 *       or stale. {@link #masterHash} is {@code 0} and ignored by the server. The server
 *       replies with {@link SyncDataS2C} containing the requested namespaces.</li>
 * </ul>
 *
 * <p>Use the factory methods {@link #fast} and {@link #slow} instead of the constructor
 * to make the intent explicit at call sites.
 *
 * @param missing    namespace keys the client needs data for; empty on the fast path
 * @param masterHash the client's computed master hash; meaningful only on the fast path
 */
public record SyncResponseC2S(List<String> missing, int masterHash) implements CustomPacketPayload {

    public static final Type<SyncResponseC2S> ID = new Type<>(PlateauSyncConfig.id("sync_response"));

    public static final StreamCodec<FriendlyByteBuf, SyncResponseC2S> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncResponseC2S::missing,
            ByteBufCodecs.VAR_INT, SyncResponseC2S::masterHash,
            SyncResponseC2S::new
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() { return ID; }

    // -------------------------------------------------------------------------
    // Factory methods — use these; do not call the constructor directly
    // -------------------------------------------------------------------------

    /**
     * Creates a fast-path response indicating the client has valid caches for all namespaces.
     *
     * @param clientMasterHash the master hash the client computed from its cached namespace hashes,
     *                         derived from {@link com.sypztep.plateau.common.v1.network.ConfigSyncRegistry#masterHash(java.util.Map)
     *                         ConfigSyncRegistry.masterHash(hashes)} using the hashes received in {@link SyncHelloS2C}
     * @return a {@link SyncResponseC2S} with an empty {@code missing} list
     */
    public static SyncResponseC2S fast(int clientMasterHash) {
        return new SyncResponseC2S(List.of(), clientMasterHash);
    }

    /**
     * Creates a slow-path response listing the namespaces the client needs data for.
     *
     * @param missingNamespaces the namespace keys whose caches are absent or stale;
     *                          must not be empty (use {@link #fast} if nothing is missing)
     * @return a {@link SyncResponseC2S} with {@code masterHash} set to {@code 0}
     */
    public static SyncResponseC2S slow(List<String> missingNamespaces) {
        return new SyncResponseC2S(List.copyOf(missingNamespaces), 0);
    }

    // -------------------------------------------------------------------------
    // Convenience accessors
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if this is a fast-path response (no missing namespaces).
     *
     * @return {@code true} when {@link #missing} is empty
     */
    public boolean isFastPath() { return missing.isEmpty(); }
}
