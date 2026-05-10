package com.sypztep.plateau.client;

import com.sypztep.plateau.client.v1.ui.theme.UIThemeRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlateauUIClient implements ClientModInitializer {
    public static Logger LOGGER = LoggerFactory.getLogger("PlateauUIClient");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("plateau-ui", path);
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("[PlateauUIClient] Initialize Plateau UI Client");
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(id(UIThemeRegistry.DIRECTORY), UIThemeRegistry.INSTANCE);
    }
}
