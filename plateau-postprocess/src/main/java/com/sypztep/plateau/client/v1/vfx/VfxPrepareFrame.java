package com.sypztep.plateau.client.v1.vfx;

import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;

/**
 * Frame context for {@link VfxEffect#prepare} — a {@link VfxFrame} plus the
 * Fabric {@link LevelRenderContext} of the level-render phase the effect
 * declared via {@link VfxEffect#preparePhase()}.
 */
@Environment(EnvType.CLIENT)
public final class VfxPrepareFrame extends VfxFrame {
    private final LevelRenderContext level;

    VfxPrepareFrame(VfxHandle handle, Minecraft mc, float partialTick,
                    GraphicsResourceAllocator allocator, LevelRenderContext level) {
        super(handle, mc, partialTick, allocator);
        this.level = level;
    }

    /** The Fabric level-render context: pose stack, buffer source, game renderer, etc. */
    public LevelRenderContext level() {
        return level;
    }
}
