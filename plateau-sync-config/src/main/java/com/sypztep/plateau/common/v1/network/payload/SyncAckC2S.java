package com.sypztep.plateau.common.v1.network.payload;

import com.sypztep.plateau.PlateauSyncConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

/**
 * <b>Client → Server.</b> Final packet in the slow-path handshake.
 *
 * <p>Sent by the client after it has successfully applied a {@link SyncDataS2C} packet.
 * Echoes the {@code masterHash} that the server included in that packet so the server
 * can verify the client applied the correct config state before unfreezing the player.
 *
 * <p>If the echoed hash does not match the server's current master hash (computed fresh
 * at ack-receive time), the server disconnects the player with an explanatory message.
 * This typically indicates network corruption or a client-side bug.
 *
 * @param masterHash the master hash echoed from {@link SyncDataS2C#masterHash()}
 */
public record SyncAckC2S(int masterHash) implements CustomPacketPayload {

    public static final Type<SyncAckC2S> ID = new Type<>(PlateauSyncConfig.id("sync_ack"));

    public static final StreamCodec<FriendlyByteBuf, SyncAckC2S> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, SyncAckC2S::masterHash, SyncAckC2S::new);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() { return ID; }
}
