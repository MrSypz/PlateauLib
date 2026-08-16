package com.sypztep.plateau.client;

import com.sypztep.plateau.client.v1.vfx.VfxLevelPhase;
import com.sypztep.plateau.client.v1.vfx.VfxManager;
import com.sypztep.plateau.client.v1.vfx.VfxManagerAccess;
import com.sypztep.plateau.client.v1.vfx.VfxMaskGroups;
import com.sypztep.plateau.client.v1.vfx.effects.VfxEffects;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlateauPostprocessClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("PlateauPostprocess");

    @Override
    public void onInitializeClient() {
        LOGGER.info("[Postprocess] Initialize post process module (API v{})", VfxManager.API_VERSION);

        VfxMaskGroups.register();
        VfxEffects.registerAll();

        // One listener per supported prepare phase — the manager fans out to
        // effects that declared the phase, in priority order.
        LevelRenderEvents.AFTER_SOLID_FEATURES.register(ctx
                -> VfxManagerAccess.dispatchPrepare(VfxLevelPhase.AFTER_SOLID_FEATURES, ctx));
        LevelRenderEvents.BEFORE_TRANSLUCENT_TERRAIN.register(ctx
                -> VfxManagerAccess.dispatchPrepare(VfxLevelPhase.BEFORE_TRANSLUCENT_TERRAIN, ctx));
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(ctx
                -> VfxManagerAccess.dispatchPrepare(VfxLevelPhase.AFTER_TRANSLUCENT_TERRAIN, ctx));
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(ctx
                -> VfxManagerAccess.dispatchPrepare(VfxLevelPhase.AFTER_TRANSLUCENT_FEATURES, ctx));

        ClientPlayConnectionEvents.DISCONNECT.register((_, client)
                -> client.execute(VfxManagerAccess::closeAll));
    }
}
