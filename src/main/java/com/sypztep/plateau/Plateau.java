package com.sypztep.plateau;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Plateau implements ModInitializer {
    public static final String MODID = "plateau";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
    @Override
    public void onInitialize() {
    }
}
