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

/**
 * Named offscreen color targets a consumer mod draws arbitrary geometry
 * into mid-level-render, so other effects (e.g. {@link
 * com.sypztep.plateau.client.v1.vfx.effects.OutlineEffect}) can restrict
 * themselves to just that silhouette instead of the whole scene. "Flagging"
 * something into a group is just the caller choosing to wrap its own draw
 * call in {@link #draw} — there is no global entity/emitter tagging registry.
 *
 * <p>Registered with {@link VfxManager} like any other effect (via {@link
 * #register()}) so groups get the same per-frame resize + disconnect cleanup
 * as every other Vfx target, but it has no {@code contribute()} output of
 * its own — it's a producer other effects read from via {@link #target}.
 *
 * <p>Groups are cleared once per frame during {@link VfxLevelPhase#AFTER_SOLID_FEATURES}
 * (the earliest phase), at the highest priority, so any effect drawing into a
 * group from a <em>later</em> phase is guaranteed to see it already cleared
 * this frame. Drawing into a group from {@code AFTER_SOLID_FEATURES} itself
 * has unspecified ordering against the clear — prefer a later phase.
 */
@Environment(EnvType.CLIENT)
public final class VfxMaskGroups implements VfxEffect {
    private static final VfxMaskGroups INSTANCE = new VfxMaskGroups();
    private static boolean registered;

    private final Map<Identifier, TextureTarget> groups = new LinkedHashMap<>();
    private int lastWidth, lastHeight;

    private VfxMaskGroups() {}

    /** Call once at client init, before any {@link #draw} call. Idempotent. */
    public static void register() {
        if (registered) return;
        registered = true;
        VfxManager.register(INSTANCE).priority(100);
    }

    /**
     * Draw into the named group's offscreen target this frame — redirects
     * batched color output for the duration of {@code drawIntoGroup} via
     * {@link VfxScope#overrideOutput}, then restores it. Safe to call from
     * any level-render phase; creates the group's target on first use.
     */
    public static void draw(Identifier group, Runnable drawIntoGroup) {
        TextureTarget target = INSTANCE.groupTarget(group);
        try (VfxScope ignored = VfxScope.overrideOutput(target.getColorTextureView())) {
            drawIntoGroup.run();
        }
    }

    /**
     * The current target for {@code group}, or {@code null} if nothing has
     * ever drawn into it. For effects reading a group as an extra {@link
     * VfxPostChain} external target.
     */
    public static RenderTarget target(Identifier group) {
        return INSTANCE.groups.get(group);
    }

    private TextureTarget groupTarget(Identifier group) {
        TextureTarget target = groups.get(group);
        if (target == null) {
            RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
            target = new TextureTarget("mask_group_" + group, main.width, main.height, false);
            clear(target);
            groups.put(group, target);
        }
        return target;
    }

    @Override
    public void init(VfxContext ctx) {
        // Groups are created lazily by draw(); nothing to allocate up front.
    }

    @Override
    public VfxLevelPhase preparePhase() {
        return VfxLevelPhase.AFTER_SOLID_FEATURES;
    }

    @Override
    public boolean prepare(VfxPrepareFrame frame) {
        RenderTarget main = frame.mc().getMainRenderTarget();
        if (main.width != lastWidth || main.height != lastHeight) {
            for (Map.Entry<Identifier, TextureTarget> entry : groups.entrySet()) {
                entry.getValue().destroyBuffers();
                TextureTarget resized = new TextureTarget("mask_group_" + entry.getKey(), main.width, main.height, false);
                groups.put(entry.getKey(), resized);
            }
            lastWidth = main.width;
            lastHeight = main.height;
        }
        for (TextureTarget target : groups.values()) clear(target);
        return false; // pure producer — no contribute() output
    }

    private void clear(TextureTarget target) {
        if (target.getColorTextureView() == null) return;
        try (RenderPass ignored = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "Clear mask group", target.getColorTextureView(),
                        OptionalInt.of(0), null, OptionalDouble.empty())) {
            // Empty pass — just clears
        }
    }

    @Override
    public FrameContribution contribute(VfxFrame frame) {
        return FrameContribution.NONE;
    }

    @Override
    public void close() {
        for (TextureTarget target : groups.values()) target.destroyBuffers();
        groups.clear();
        lastWidth = 0;
        lastHeight = 0;
    }
}
