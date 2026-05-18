package com.sypztep.plateau.test;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttributesTestMod implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("AttributesTestMod");

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(_ -> verifyRegistry());
        ServerPlayConnectionEvents.JOIN.register((handler, _, _) -> verifyPlayer(handler.player));
    }

    private static void verifyRegistry() {
        boolean livingBound = TestAttributes.TEST_LIVING.isBound();
        boolean playerBound = TestAttributes.TEST_PLAYER.isBound();

        if (livingBound && playerBound) {
            LOGGER.info("[AttributesTest] PASS registry — TEST_LIVING bound={}, TEST_PLAYER bound={}", livingBound, playerBound);
        } else {
            LOGGER.error("[AttributesTest] FAIL registry — TEST_LIVING bound={}, TEST_PLAYER bound={}", livingBound, playerBound);
        }
    }

    private static void verifyPlayer(ServerPlayer player) {
        boolean hasLiving = player.getAttributes().hasAttribute(TestAttributes.TEST_LIVING);
        boolean hasPlayer = player.getAttributes().hasAttribute(TestAttributes.TEST_PLAYER);

        if (hasLiving && hasPlayer) {
            LOGGER.info("[AttributesTest] PASS player '{}' — TEST_LIVING={}, TEST_PLAYER={}", player.getName().getString(), hasLiving, hasPlayer);
        } else {
            LOGGER.error("[AttributesTest] FAIL player '{}' — TEST_LIVING={}, TEST_PLAYER={}", player.getName().getString(), hasLiving, hasPlayer);
        }
    }
}
