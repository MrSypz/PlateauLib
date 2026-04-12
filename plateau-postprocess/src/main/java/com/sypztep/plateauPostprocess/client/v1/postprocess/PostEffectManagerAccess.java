package com.sypztep.plateauPostprocess.client.v1.postprocess;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;

/**
 * Engine-internal bridge exposing lifecycle hooks to mixins.
 *
 * <p>This class is NOT part of the public API. It exists solely so that
 * {@code GameRendererMixin} (which lives outside this package) can call
 * the engine's frame and disconnect hooks without exposing them publicly.
 *
 * @apiNote Internal — do not use in your mod code.
 */
@Environment(EnvType.CLIENT)
public final class PostEffectManagerAccess {
    private PostEffectManagerAccess() {}

    /** @see PostEffectManager#applyAll */
    @ApiStatus.Internal
    public static void applyAll(Minecraft mc, float partialTick) {
        PostEffectManager.applyAll(mc, partialTick);
    }

    /** @see PostEffectManager#closeAll */
    @ApiStatus.Internal
    public static void closeAll() {
        PostEffectManager.closeAll();
    }
}