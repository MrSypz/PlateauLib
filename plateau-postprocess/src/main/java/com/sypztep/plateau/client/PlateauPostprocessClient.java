package com.sypztep.plateau.client;

import com.sypztep.plateau.client.v1.postprocess.PostEffectManager;
import com.sypztep.plateau.client.v1.postprocess.PostEffectManagerAccess;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlateauPostprocessClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("PlateauPostprocess");

    @Override
    public void onInitializeClient() {
        LOGGER.info("[Postprocess] Initialize post process module (API v{})", PostEffectManager.API_VERSION);
        ClientPlayConnectionEvents.DISCONNECT.register((_, client)
                -> client.execute(PostEffectManagerAccess::closeAll));
    }
}
