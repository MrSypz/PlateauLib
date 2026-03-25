package com.sypztep.plateau.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sypztep.plateau.client.impl.network.AddEmitterParticlePayloadS2C;
import com.sypztep.plateau.client.impl.network.AddParticlePayloadS2C;
import com.sypztep.plateau.client.impl.network.AddTextParticlePayloadS2C;
import com.sypztep.plateau.client.impl.particle.state.TextParticleGroup;
import com.sypztep.plateau.client.impl.ui.TestScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleRendererRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import org.lwjgl.glfw.GLFW;

public class PlateauClient implements ClientModInitializer {
    public static final ParticleRenderType TEXT_PARTICLE = new ParticleRenderType("text_particle");
    public static final KeyMapping TEST_SCREEN_KEY = new KeyMapping(
            "key.plateau.test_screen", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, KeyMapping.Category.DEBUG);

    @Override
    public void onInitializeClient() {
        ParticleRendererRegistry.register(TEXT_PARTICLE, TextParticleGroup::new);

        ClientPlayNetworking.registerGlobalReceiver(AddTextParticlePayloadS2C.ID, new AddTextParticlePayloadS2C.Receiver());
        ClientPlayNetworking.registerGlobalReceiver(AddEmitterParticlePayloadS2C.ID, new AddEmitterParticlePayloadS2C.Receiver());
        ClientPlayNetworking.registerGlobalReceiver(AddParticlePayloadS2C.ID, new AddParticlePayloadS2C.Receiver());

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (TEST_SCREEN_KEY.consumeClick()) client.setScreen(new TestScreen());
            });
        }
    }
}
