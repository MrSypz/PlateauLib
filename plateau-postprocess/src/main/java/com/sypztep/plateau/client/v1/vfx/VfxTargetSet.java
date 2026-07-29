package com.sypztep.plateau.client.v1.vfx;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Owns N named render targets — screen-sized ones track the main framebuffer
 * (destroy + recreate on resize, per MC's own {@code RenderTarget} contract),
 * fixed ones keep their dimensions. Register via {@link VfxContext#ownTargets}
 * so the manager sizes them once per frame before the effect runs and closes
 * them on disconnect.
 */
@Environment(EnvType.CLIENT)
public final class VfxTargetSet implements AutoCloseable {

    private record Spec(String label, boolean useDepth, int fixedWidth, int fixedHeight) {
        boolean screenSized() {
            return fixedWidth <= 0;
        }
    }

    private final Map<Identifier, Spec> specs;
    private final Map<Identifier, TextureTarget> targets = new LinkedHashMap<>();
    private int lastWidth;
    private int lastHeight;

    private VfxTargetSet(Map<Identifier, Spec> specs) {
        this.specs = specs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<Identifier, Spec> specs = new LinkedHashMap<>();

        /** A screen-sized target that tracks the main framebuffer's dimensions. */
        public Builder target(Identifier id, String label, boolean useDepth) {
            specs.put(id, new Spec(label, useDepth, 0, 0));
            return this;
        }

        /** A fixed-size target (e.g. a shadow face or voxel grid slice). */
        public Builder fixedTarget(Identifier id, String label, int width, int height, boolean useDepth) {
            specs.put(id, new Spec(label, useDepth, width, height));
            return this;
        }

        public VfxTargetSet build() {
            if (specs.isEmpty()) throw new IllegalStateException("VfxTargetSet needs at least one target");
            return new VfxTargetSet(new LinkedHashMap<>(specs));
        }
    }

    public Set<Identifier> ids() {
        return specs.keySet();
    }

    /** The current target for {@code id} — non-null once {@link #ensureSized} has run this frame. */
    public RenderTarget get(Identifier id) {
        return targets.get(id);
    }

    /**
     * Create/resize every target. Called once per frame by the manager for
     * sets registered via {@link VfxContext#ownTargets} — consumers don't
     * call this.
     */
    public void ensureSized(Minecraft mc) {
        RenderTarget main = mc.getMainRenderTarget();
        boolean screenChanged = main.width != lastWidth || main.height != lastHeight;
        for (Map.Entry<Identifier, Spec> entry : specs.entrySet()) {
            Spec spec = entry.getValue();
            TextureTarget existing = targets.get(entry.getKey());
            if (existing != null && !(spec.screenSized() && screenChanged)) continue;
            if (existing != null) existing.destroyBuffers();
            int w = spec.screenSized() ? main.width : spec.fixedWidth();
            int h = spec.screenSized() ? main.height : spec.fixedHeight();
            targets.put(entry.getKey(), new TextureTarget(spec.label(), w, h, spec.useDepth()));
        }
        lastWidth = main.width;
        lastHeight = main.height;
    }

    /** Clear a target to transparent black (and depth 1.0 if it has a depth buffer). */
    public void clear(Identifier id) {
        TextureTarget target = targets.get(id);
        if (target == null || target.getColorTextureView() == null) return;
        try (RenderPass ignored = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(
                        () -> "Clear " + id,
                        target.getColorTextureView(),
                        OptionalInt.of(0),
                        target.useDepth ? target.getDepthTextureView() : null,
                        target.useDepth ? OptionalDouble.of(1.0) : OptionalDouble.empty()
                )) {
            // Empty pass — just clears
        }
    }

    @Override
    public void close() {
        for (TextureTarget target : targets.values()) target.destroyBuffers();
        targets.clear();
        lastWidth = 0;
        lastHeight = 0;
    }
}
