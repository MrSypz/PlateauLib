package com.sypztep.plateau;

import com.sypztep.plateau.client.v1.network.AddEmitterParticlePayloadS2C;
import com.sypztep.plateau.client.v1.network.AddParticlePayloadS2C;
import com.sypztep.plateau.client.v1.network.AddTextParticlePayloadS2C;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class PlateauParticles implements ModInitializer {
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(AddTextParticlePayloadS2C.ID, AddTextParticlePayloadS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AddEmitterParticlePayloadS2C.ID, AddEmitterParticlePayloadS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AddParticlePayloadS2C.ID, AddParticlePayloadS2C.CODEC);
    }
}
