package com.sypztep.plateau.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlateauUIClient implements ClientModInitializer {
    public static Logger LOGGER = LoggerFactory.getLogger("PlateauUIClient");
    @Override
    public void onInitializeClient() {
        LOGGER.info("[PlateauUIClient] Initialize Plateau UI Client");
    }
}
