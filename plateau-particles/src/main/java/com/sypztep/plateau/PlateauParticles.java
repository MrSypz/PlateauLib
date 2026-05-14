package com.sypztep.plateau;

import com.sypztep.plateau.common.v1.network.AddEmitterParticlePayloadS2C;
import com.sypztep.plateau.common.v1.network.AddParticlePayloadS2C;
import com.sypztep.plateau.common.v1.network.AddTextParticlePayloadS2C;
import com.sypztep.plateau.common.v1.network.AddTypedTextParticlePayloadS2C;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;

public class PlateauParticles implements ModInitializer {
    public static final String MODID = "plateau-particles";
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(AddTextParticlePayloadS2C.ID, AddTextParticlePayloadS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AddTypedTextParticlePayloadS2C.ID, AddTypedTextParticlePayloadS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AddEmitterParticlePayloadS2C.ID, AddEmitterParticlePayloadS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AddParticlePayloadS2C.ID, AddParticlePayloadS2C.CODEC);
    }
}
