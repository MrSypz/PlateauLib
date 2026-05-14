package com.sypztep.plateau.common.v1.network;

import com.sypztep.plateau.PlateauParticles;
import com.sypztep.plateau.client.v1.particle.TextParticle;
import com.sypztep.plateau.common.v1.TextParticleRegister;
import com.sypztep.plateau.common.v1.TextParticleType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

public record AddTypedTextParticlePayloadS2C(int entityId, Identifier typeId) implements CustomPacketPayload {

    public static final Type<AddTypedTextParticlePayloadS2C> ID = new Type<>(PlateauParticles.id("add_typed_text_particle"));

    public static final StreamCodec<FriendlyByteBuf, AddTypedTextParticlePayloadS2C> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AddTypedTextParticlePayloadS2C::entityId,
            Identifier.STREAM_CODEC, AddTypedTextParticlePayloadS2C::typeId,
            AddTypedTextParticlePayloadS2C::new
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void send(ServerPlayer receiver, int entityId, TextParticleType type) {
        ServerPlayNetworking.send(receiver, new AddTypedTextParticlePayloadS2C(entityId, type.id()));
    }

    public static class Receiver implements ClientPlayNetworking.PlayPayloadHandler<AddTypedTextParticlePayloadS2C> {
        @Override
        public void receive(AddTypedTextParticlePayloadS2C payload, ClientPlayNetworking.@NonNull Context context) {
            TextParticleType type = TextParticleRegister.getType(payload.typeId());
            if (type == null || !type.isEnabled()) return;

            Minecraft client = Minecraft.getInstance();
            ClientLevel world = client.level;
            if (world == null) return;

            Entity entity = world.getEntity(payload.entityId());
            if (entity == null) return;

            Vec3 pos = entity.position().add(0, entity.getBbHeight() + 0.95 + type.yPos(), 0);
            TextParticle particle = new TextParticle(world, pos.x, pos.y, pos.z);
            particle.setText(Component.translatable(type.translationKey()).getString());
            particle.setColor(type.color());
            particle.setMaxSize(type.maxSize());
            client.particleEngine.add(particle);
        }
    }
}
