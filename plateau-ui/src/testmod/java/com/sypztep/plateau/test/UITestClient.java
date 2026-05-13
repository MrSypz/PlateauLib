package com.sypztep.plateau.test;

import com.mojang.blaze3d.platform.InputConstants;
import com.sypztep.plateau.test.screen.UITestScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class UITestClient implements ClientModInitializer {
    public static final String MODID = "plateau-ui-testmod";
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID,path);
    }
    public static final KeyMapping.Category DEBUG = KeyMapping.Category.register(id("debug"));

    @Override
    public void onInitializeClient() {
        final KeyMapping OPENSCREEN = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.leklai.sit", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, DEBUG));

        ClientTickEvents.END_CLIENT_TICK.register(_ -> {
            while (OPENSCREEN.consumeClick()) {
                Minecraft.getInstance().setScreen(new UITestScreen());
            }
        });
    }
}
