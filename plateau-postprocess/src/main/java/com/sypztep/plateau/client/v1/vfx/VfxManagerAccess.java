package com.sypztep.plateau.client.v1.vfx;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;

/**
 * Engine-internal bridge exposing lifecycle hooks to mixins and the module's
 * client entrypoint (which live outside this package).
 *
 * @apiNote Internal — do not use in your mod code.
 */
@Environment(EnvType.CLIENT)
public final class VfxManagerAccess {
    private VfxManagerAccess() {}

    /** @see VfxManager#applyAll */
    @ApiStatus.Internal
    public static void applyAll(Minecraft mc, float partialTick) {
        VfxManager.applyAll(mc, partialTick);
    }

    /** @see VfxManager#dispatchPrepare */
    @ApiStatus.Internal
    public static void dispatchPrepare(VfxLevelPhase phase, LevelRenderContext context) {
        VfxManager.dispatchPrepare(phase, context);
    }

    /** @see VfxManager#closeAll */
    @ApiStatus.Internal
    public static void closeAll() {
        VfxManager.closeAll();
    }
}
