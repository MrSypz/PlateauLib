package com.sypztep.plateau;

import com.sypztep.plateau.client.impl.network.AddEmitterParticlePayloadS2C;
import com.sypztep.plateau.client.impl.network.AddParticlePayloadS2C;
import com.sypztep.plateau.client.impl.network.AddTextParticlePayloadS2C;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Plateau implements ModInitializer {
    public static final String MODID = "plateau";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(AddTextParticlePayloadS2C.ID, AddTextParticlePayloadS2C.CODEC);
        PayloadTypeRegistry.playS2C().register(AddEmitterParticlePayloadS2C.ID, AddEmitterParticlePayloadS2C.CODEC);
        PayloadTypeRegistry.playS2C().register(AddParticlePayloadS2C.ID, AddParticlePayloadS2C.CODEC);
    }
}
