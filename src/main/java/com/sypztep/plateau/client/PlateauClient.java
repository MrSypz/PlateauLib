package com.sypztep.plateau.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sypztep.plateau.client.ui.TestScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class PlateauClient implements ClientModInitializer {
//    public static KeyMapping stats_screen = new KeyMapping("key.dominatus.debug", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, KeyMapping.Category.DEBUG);

    @Override
    public void onInitializeClient() {
//        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
//            ClientTickEvents.END_CLIENT_TICK.register(PlateauClient::onEndTick);
//        }
    }

    private static void onEndTick(Minecraft client) {
//        if (stats_screen.consumeClick()) client.setScreen(new TestScreen());
    }
}
