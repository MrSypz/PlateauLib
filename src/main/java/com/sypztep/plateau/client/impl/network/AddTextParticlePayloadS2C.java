package com.sypztep.plateau.client.impl.network;

import com.sypztep.plateau.Plateau;
import com.sypztep.plateau.client.impl.particle.TextParticle;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public record AddTextParticlePayloadS2C(int entityId, Component text, int color, float maxSize,
                                        float yPos) implements CustomPacketPayload {

    public static final Type<AddTextParticlePayloadS2C> ID = new Type<>(Plateau.id("add_text_particle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AddTextParticlePayloadS2C> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, AddTextParticlePayloadS2C::entityId, ComponentSerialization.STREAM_CODEC, AddTextParticlePayloadS2C::text, ByteBufCodecs.INT, AddTextParticlePayloadS2C::color, ByteBufCodecs.FLOAT, AddTextParticlePayloadS2C::maxSize, ByteBufCodecs.FLOAT, AddTextParticlePayloadS2C::yPos, AddTextParticlePayloadS2C::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void send(ServerPlayer receiver, int entityId, Component text, int color, float maxSize, float yPos) {
        ServerPlayNetworking.send(receiver, new AddTextParticlePayloadS2C(entityId, text, color, maxSize, yPos));
    }

    public static class Receiver implements ClientPlayNetworking.PlayPayloadHandler<AddTextParticlePayloadS2C> {
        @Override
        public void receive(AddTextParticlePayloadS2C payload, ClientPlayNetworking.Context context) {
            Entity entity = context.player().level().getEntity(payload.entityId());
            if (entity == null) return;

            Minecraft client = Minecraft.getInstance();
            ClientLevel world = client.level;
            if (world == null) return;

            Vec3 pos = entity.position().add(0, entity.getBbHeight() + 0.95 + payload.yPos(), 0);
            TextParticle particle = new TextParticle(world, pos.x, pos.y, pos.z);
            particle.setText(payload.text().getString());
            particle.setColor(payload.color());
            particle.setMaxSize(payload.maxSize());

            client.particleEngine.add(particle);
        }
    }
}