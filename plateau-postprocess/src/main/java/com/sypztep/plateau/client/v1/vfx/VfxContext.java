package com.sypztep.plateau.client.v1.vfx;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

/**
 * Handed to {@link VfxEffect#init}. Resources registered here are owned by the
 * framework: closed automatically on disconnect/unregister, in reverse
 * registration order, after the effect's own {@link VfxEffect#close()} runs —
 * no hand-managed nullable statics per consumer.
 */
@Environment(EnvType.CLIENT)
public final class VfxContext {
    private final VfxHandle handle;
    private final Minecraft mc;

    VfxContext(VfxHandle handle, Minecraft mc) {
        this.handle = handle;
        this.mc = mc;
    }

    public Minecraft mc() {
        return mc;
    }

    /** Register a resource for automatic close on disconnect/unregister. Returns it for chaining. */
    public <T extends AutoCloseable> T own(T resource) {
        handle.own(resource);
        return resource;
    }

    /**
     * Like {@link #own}, and additionally the manager calls
     * {@link VfxTargetSet#ensureSized(Minecraft)} once per frame before this
     * effect runs, so targets always match the main framebuffer before any
     * mask drawing happens.
     */
    public VfxTargetSet ownTargets(VfxTargetSet targets) {
        handle.ownTargets(targets);
        return targets;
    }
}
