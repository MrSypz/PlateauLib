package com.sypztep.plateau.client;

import com.sypztep.plateau.client.impl.particle.state.TextParticleGroup;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;

public class PlateauClient implements ClientModInitializer {
//    public static KeyMapping stats_screen = new KeyMapping("key.dominatus.debug", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, KeyMapping.Category.DEBUG);
    public static final ParticleRenderType TEXT_PARTICLE =  new ParticleRenderType("plateau:text_particle");
    @Override
    public void onInitializeClient() {

        ParticleRendererRegistry.register(TEXT_PARTICLE, TextParticleGroup::new);

//        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
//            ClientTickEvents.END_CLIENT_TICK.register(PlateauClient::onEndTick);
//        }
    }

    private static void onEndTick(Minecraft client) {
//        if (stats_screen.consumeClick()) client.setScreen(new TestScreen());
    }
}
