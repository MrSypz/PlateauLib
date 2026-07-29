package com.sypztep.plateau.client.v1.vfx;

import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

/**
 * Per-frame context handed to {@link VfxEffect#contribute}. Wraps everything
 * an effect needs to run a post chain this frame, plus a diagnostics channel
 * ({@link #skip(String)}) so "why didn't this run" is never silent.
 */
@Environment(EnvType.CLIENT)
public sealed class VfxFrame permits VfxPrepareFrame {
    private final VfxHandle handle;
    private final Minecraft mc;
    private final float partialTick;
    private final GraphicsResourceAllocator allocator;

    VfxFrame(VfxHandle handle, Minecraft mc, float partialTick, GraphicsResourceAllocator allocator) {
        this.handle = handle;
        this.mc = mc;
        this.partialTick = partialTick;
        this.allocator = allocator;
    }

    public Minecraft mc() {
        return mc;
    }

    public float partialTick() {
        return partialTick;
    }

    /** Pooled allocator for transient frame-graph resources. */
    public GraphicsResourceAllocator allocator() {
        return allocator;
    }

    /**
     * Record why this effect skipped this frame — surfaced through
     * {@link VfxHandle#diagnostics()}. Called automatically by
     * {@link VfxPostChain} when a chain or external target is unavailable.
     */
    public void skip(String reason) {
        handle.setSkipReason(reason);
    }
}
