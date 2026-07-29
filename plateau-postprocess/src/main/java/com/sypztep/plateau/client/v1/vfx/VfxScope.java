package com.sypztep.plateau.client.v1.vfx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Try-with-resources guard for {@code RenderSystem.outputColorTextureOverride}
 * — redirects batched draws into an offscreen target (mask rendering) and
 * guarantees the override is restored even on exception, so the unguarded
 * push/pop pattern can't be written incorrectly.
 *
 * <pre>{@code
 * bufferSource.endLastBatch();
 * try (var scope = VfxScope.overrideOutput(mask.getColorTextureView())) {
 *     drawMask(bufferSource);
 *     bufferSource.endBatch(MASK_RENDER_TYPE);
 * }
 * }</pre>
 */
@Environment(EnvType.CLIENT)
public final class VfxScope implements AutoCloseable {
    private final GpuTextureView previous;

    private VfxScope(GpuTextureView previous) {
        this.previous = previous;
    }

    public static VfxScope overrideOutput(GpuTextureView target) {
        VfxScope scope = new VfxScope(RenderSystem.outputColorTextureOverride);
        RenderSystem.outputColorTextureOverride = target;
        return scope;
    }

    @Override
    public void close() {
        RenderSystem.outputColorTextureOverride = previous;
    }
}
