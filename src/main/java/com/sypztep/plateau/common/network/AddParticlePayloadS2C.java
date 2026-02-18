package com.sypztep.plateau.common.network;

import com.sypztep.plateau.Plateau;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

/**
 * @param config
 * <p>byte 1 as overrideLimiter </p>
 * <p>byte 2 as alwaysShow</p>
 */
public record AddParticlePayloadS2C(int entityId, ParticleOptions particleOptions, Vec3 pos, Vec3 velocity,
                                    byte config) implements CustomPacketPayload {
    public static final Type<AddParticlePayloadS2C> ID = new Type<>(Plateau.id("add_particle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AddParticlePayloadS2C> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AddParticlePayloadS2C::entityId,
            ParticleTypes.STREAM_CODEC, AddParticlePayloadS2C::particleOptions,
            Vec3.STREAM_CODEC, AddParticlePayloadS2C::pos,
            Vec3.STREAM_CODEC, AddParticlePayloadS2C::velocity,
            ByteBufCodecs.BYTE, AddParticlePayloadS2C::config,
            AddParticlePayloadS2C::new
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
    public static void send(ServerPlayer receiver, int entityId, ParticleOptions particle, Vec3 pos, Vec3 velocity, byte config) {
        ServerPlayNetworking.send(receiver, new AddParticlePayloadS2C(entityId, particle, pos, velocity, config));
    }

    public static class Receiver implements ClientPlayNetworking.PlayPayloadHandler<AddParticlePayloadS2C> {
        @Override
        public void receive(AddParticlePayloadS2C payload, ClientPlayNetworking.Context context) {
            ClientLevel client = context.client().level;
            if (client != null) {
                client.addParticle(
                        payload.particleOptions,
                        (payload.config & 1) != 0,
                        (payload.config & 2) != 0,
                        payload.pos.x, payload.pos.y, payload.pos.z,
                        payload.velocity.x, payload.velocity.y, payload.velocity.z
                );
            }
        }
    }
}