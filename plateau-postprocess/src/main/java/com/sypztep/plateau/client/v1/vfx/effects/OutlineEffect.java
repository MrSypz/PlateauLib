package com.sypztep.plateau.client.v1.vfx.effects;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.sypztep.plateau.client.v1.vfx.FrameContribution;
import com.sypztep.plateau.client.v1.vfx.ManagedUniform;
import com.sypztep.plateau.client.v1.vfx.VfxContext;
import com.sypztep.plateau.client.v1.vfx.VfxEffect;
import com.sypztep.plateau.client.v1.vfx.VfxFrame;
import com.sypztep.plateau.client.v1.vfx.VfxLevelPhase;
import com.sypztep.plateau.client.v1.vfx.VfxMaskGroups;
import com.sypztep.plateau.client.v1.vfx.VfxPostChain;
import com.sypztep.plateau.client.v1.vfx.VfxPrepareFrame;
import com.sypztep.plateau.client.v1.vfx.VfxTargetSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in outline post effect (see {@link VfxEffects#outline()}). Default
 * (no mask group requested) is a whole-scene depth-discontinuity edge detect
 * (like vanilla's entity_sobel, but on depth instead of alpha). Passing a
 * {@link com.sypztep.plateau.client.v1.vfx.VfxMaskGroups} group name via
 * {@link OutlineParams#withMaskGroup} restricts outlining to just that
 * group's drawn silhouette instead of every depth edge in the scene (Phase
 * 2). Same request/blend pattern as {@link VignetteEffect}; when requests in
 * one frame mix a mask group and none, the first non-null group wins for the
 * whole frame — the numeric params (thickness/depthSensitivity/color) still
 * blend normally.
 *
 * <p>Reads a mid-level depth snapshot, not {@code minecraft:main}'s live
 * depth — same fix and same reason as {@link DepthOfFieldEffect}: by
 * {@link #contribute}'s time the main target's depth has already been
 * cleared and overwritten by the first-person hand render. See that class's
 * doc for the exact call chain.
 */
@Environment(EnvType.CLIENT)
public final class OutlineEffect implements VfxEffect {
    private static final Identifier CHAIN_ID = Identifier.fromNamespaceAndPath("plateau-postprocess", "outline");
    private static final Identifier MASKED_CHAIN_ID = Identifier.fromNamespaceAndPath("plateau-postprocess", "outline_masked");
    private static final Identifier DEPTH_SNAPSHOT_ID = Identifier.fromNamespaceAndPath("plateau-postprocess", "outline_depth_snapshot");
    private static final Identifier MASK_TARGET_ID = Identifier.fromNamespaceAndPath("plateau-postprocess", "outline_mask_input");
    private static final int UNIFORM_BYTES = 32; // std140: float Thickness, float DepthSensitivity, vec3 Color

    private record Weighted(float weight, OutlineParams params) {}

    private final List<Weighted> requests = new ArrayList<>();

    private VfxTargetSet depthSnapshot;
    private VfxPostChain chain;
    private ManagedUniform uniform;
    private VfxPostChain maskedChain;
    private ManagedUniform maskedUniform;
    private Identifier activeMaskGroup;

    public void requestFrame(float weight, OutlineParams params) {
        if (weight <= 0f) return;
        requests.add(new Weighted(weight, params));
    }

    @Override
    public void init(VfxContext ctx) {
        depthSnapshot = ctx.ownTargets(VfxTargetSet.builder()
                .target(DEPTH_SNAPSHOT_ID, "outline_depth_snapshot", true)
                .build());
        chain = ctx.own(VfxPostChain.builder(CHAIN_ID)
                .target(DEPTH_SNAPSHOT_ID, () -> depthSnapshot.get(DEPTH_SNAPSHOT_ID))
                .build());
        uniform = chain.uniform("OutlineConfig", UNIFORM_BYTES);

        maskedChain = ctx.own(VfxPostChain.builder(MASKED_CHAIN_ID)
                .target(DEPTH_SNAPSHOT_ID, () -> depthSnapshot.get(DEPTH_SNAPSHOT_ID))
                .target(MASK_TARGET_ID, () -> activeMaskGroup == null ? null : VfxMaskGroups.target(activeMaskGroup))
                .build());
        maskedUniform = maskedChain.uniform("OutlineConfig", UNIFORM_BYTES);
    }

    @Override
    public VfxLevelPhase preparePhase() {
        return VfxLevelPhase.AFTER_TRANSLUCENT_FEATURES;
    }

    @Override
    public boolean prepare(VfxPrepareFrame frame) {
        RenderTarget main = frame.mc().getMainRenderTarget();
        RenderTarget snapshot = depthSnapshot.get(DEPTH_SNAPSHOT_ID);
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                main.getDepthTexture(), snapshot.getDepthTexture(), 0, 0, 0, 0, 0, main.width, main.height);
        return true;
    }

    @Override
    public FrameContribution contribute(VfxFrame frame) {
        if (requests.isEmpty()) return FrameContribution.NONE;

        float totalWeight = 0f, thickness = 0f, depthSensitivity = 0f, r = 0f, g = 0f, b = 0f;
        Identifier maskGroup = null;
        for (Weighted w : requests) {
            totalWeight += w.weight();
            thickness += w.weight() * w.params().thickness();
            depthSensitivity += w.weight() * w.params().depthSensitivity();
            r += w.weight() * w.params().colorR();
            g += w.weight() * w.params().colorG();
            b += w.weight() * w.params().colorB();
            if (maskGroup == null) maskGroup = w.params().maskGroup();
        }
        requests.clear();
        if (totalWeight <= 0.0001f) return FrameContribution.NONE;

        float finalThickness = thickness / totalWeight;
        float finalDepthSensitivity = depthSensitivity / totalWeight;
        float finalR = r / totalWeight, finalG = g / totalWeight, finalB = b / totalWeight;

        VfxPostChain activeChain;
        ManagedUniform activeUniform;
        if (maskGroup != null) {
            activeMaskGroup = maskGroup;
            activeChain = maskedChain;
            activeUniform = maskedUniform;
        } else {
            activeChain = chain;
            activeUniform = uniform;
        }

        if (!activeChain.prepare(frame)) return FrameContribution.NONE;
        activeUniform.write(builder -> builder.putFloat(finalThickness).putFloat(finalDepthSensitivity)
                .putVec3(finalR, finalG, finalB));
        activeChain.run(frame);
        return FrameContribution.RAN;
    }

    @Override
    public void close() {
        requests.clear();
    }
}
