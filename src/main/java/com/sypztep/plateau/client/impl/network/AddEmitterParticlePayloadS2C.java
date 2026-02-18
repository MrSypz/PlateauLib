package com.sypztep.plateau.client.impl.network;

import com.sypztep.plateau.Plateau;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.NonNull;

public record AddEmitterParticlePayloadS2C(int entityId, ParticleType<?> particleType) implements CustomPacketPayload {
    public static final Type<AddEmitterParticlePayloadS2C> ID = new Type<>(Plateau.id("add_emitter_particle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AddEmitterParticlePayloadS2C> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            AddEmitterParticlePayloadS2C::entityId,
            ByteBufCodecs.fromCodecWithRegistries(BuiltInRegistries.PARTICLE_TYPE.byNameCodec()),
            AddEmitterParticlePayloadS2C::particleType,
            AddEmitterParticlePayloadS2C::new
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void send(ServerPlayer receiver, int entityId, ParticleType<?> particleType) {
        ServerPlayNetworking.send(receiver, new AddEmitterParticlePayloadS2C(entityId, particleType));
    }

    public static class Receiver implements ClientPlayNetworking.PlayPayloadHandler<AddEmitterParticlePayloadS2C> {
        @Override
        public void receive(AddEmitterParticlePayloadS2C payload, ClientPlayNetworking.Context context) {
            Entity entity = context.player().level().getEntity(payload.entityId());
            if (entity != null) {
                context.client().particleEngine.createTrackingEmitter(entity, (ParticleOptions) payload.particleType());
            }
        }
    }
}