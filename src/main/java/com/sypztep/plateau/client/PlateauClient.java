package com.sypztep.plateau.client;

import com.sypztep.plateau.client.impl.network.AddEmitterParticlePayloadS2C;
import com.sypztep.plateau.client.impl.network.AddParticlePayloadS2C;
import com.sypztep.plateau.client.impl.network.AddTextParticlePayloadS2C;
import com.sypztep.plateau.client.impl.particle.state.TextParticleGroup;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;

public class PlateauClient implements ClientModInitializer {
    public static final ParticleRenderType TEXT_PARTICLE = new ParticleRenderType("text_particle");
//    public static KeyMapping stats_screen = new KeyMapping("key.dominatus.debug", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, KeyMapping.Category.DEBUG);

    @Override
    public void onInitializeClient() {
        ParticleRendererRegistry.register(TEXT_PARTICLE, TextParticleGroup::new);

        ClientPlayNetworking.registerGlobalReceiver(AddTextParticlePayloadS2C.ID, new AddTextParticlePayloadS2C.Receiver());
        ClientPlayNetworking.registerGlobalReceiver(AddEmitterParticlePayloadS2C.ID, new AddEmitterParticlePayloadS2C.Receiver());
        ClientPlayNetworking.registerGlobalReceiver(AddParticlePayloadS2C.ID, new AddParticlePayloadS2C.Receiver());

//        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
//            ClientTickEvents.END_CLIENT_TICK.register(PlateauClient::onEndTick);
//        }
    }

    private static void onEndTick(Minecraft client) {
//        if (stats_screen.consumeClick()) client.setScreen(new TestScreen());
    }
}
