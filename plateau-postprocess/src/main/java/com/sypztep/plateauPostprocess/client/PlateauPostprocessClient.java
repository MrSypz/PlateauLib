package com.sypztep.plateauPostprocess.client;

import com.sypztep.plateauPostprocess.client.postprocess.PostEffectManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlateauPostprocessClient implements ClientModInitializer {
    public static final String MODID = "plateau_postprocess";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[PlateauPostprocess] Initialize post process module");

        ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> PostEffectManager.closeAll());
    }
}
