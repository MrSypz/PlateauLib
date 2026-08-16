package com.sypztep.plateau.client.v1.vfx.effects;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.sypztep.plateau.client.v1.vfx.FrameContribution;
import com.sypztep.plateau.client.v1.vfx.ManagedUniform;
import com.sypztep.plateau.client.v1.vfx.VfxContext;
import com.sypztep.plateau.client.v1.vfx.VfxEffect;
import com.sypztep.plateau.client.v1.vfx.VfxFrame;
import com.sypztep.plateau.client.v1.vfx.VfxLevelPhase;
import com.sypztep.plateau.client.v1.vfx.VfxPostChain;
import com.sypztep.plateau.client.v1.vfx.VfxPrepareFrame;
import com.sypztep.plateau.client.v1.vfx.VfxTargetSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in depth of field post effect (see {@link VfxEffects#depthOfField()}).
 *
 * <p><b>Reads a depth snapshot taken mid-level, not the main target's live
 * depth.</b> By the time {@link #contribute} runs (after {@code renderLevel()}
 * returns), Minecraft has already cleared the main target's depth buffer
 * back to 1.0 and drawn the first-person hand/held item into it (see
 * {@code GameRenderer.renderLevel(DeltaTracker)} — {@code clearDepthTexture}
 * immediately followed by {@code renderItemInHand}, both still inside that
 * method). Sampling {@code minecraft:main}'s depth at that point mostly
 * returns "1.0 everywhere except where the hand is drawn," not world depth —
 * which is why an early version of this effect stayed blurry pointed at
 * anything except the player's own hand, no matter the focus distance. Fixed
 * by declaring {@link #preparePhase()} = {@link VfxLevelPhase#AFTER_TRANSLUCENT_FEATURES}
 * (the latest phase, still inside {@code LevelRenderer.renderLevel(...)},
 * strictly before the hand's depth clear) and copying the main target's
 * depth texture into an owned target there, every frame; {@link #contribute}
 * reads that snapshot instead of {@code minecraft:main}'s depth. {@link OutlineEffect}
 * has the identical fix for the identical reason.
 *
 * <p>Also computes and feeds the camera's near/far planes into the chain
 * each frame (mirroring {@code Camera#update}'s own far-plane formula, since
 * nothing exposes it directly to post-chain shaders) so
 * {@link DepthOfFieldParams#focusDistance} can be expressed in real blocks
 * instead of raw depth. Same request/blend pattern as {@link VignetteEffect}.
 */
@Environment(EnvType.CLIENT)
public final class DepthOfFieldEffect implements VfxEffect {
    private static final Identifier CHAIN_ID = Identifier.fromNamespaceAndPath("plateau-postprocess", "depth_of_field");
    private static final Identifier DEPTH_SNAPSHOT_ID = Identifier.fromNamespaceAndPath("plateau-postprocess", "dof_depth_snapshot");
    private static final int UNIFORM_BYTES = 32;

    private record Weighted(float weight, DepthOfFieldParams params) {}

    private final List<Weighted> requests = new ArrayList<>();

    private VfxTargetSet depthSnapshot;
    private VfxPostChain chain;
    private ManagedUniform hRadius;
    private ManagedUniform vRadius;
    private ManagedUniform dofConfig;

    public void requestFrame(float weight, DepthOfFieldParams params) {
        if (weight <= 0f) return;
        requests.add(new Weighted(weight, params));
    }

    @Override
    public void init(VfxContext ctx) {
        depthSnapshot = ctx.ownTargets(VfxTargetSet.builder()
                .target(DEPTH_SNAPSHOT_ID, "dof_depth_snapshot", true)
                .build());
        chain = ctx.own(VfxPostChain.builder(CHAIN_ID)
                .target(DEPTH_SNAPSHOT_ID, () -> depthSnapshot.get(DEPTH_SNAPSHOT_ID))
                .build());
        hRadius = chain.uniform("BlurHConfig", UNIFORM_BYTES);
        vRadius = chain.uniform("BlurVConfig", UNIFORM_BYTES);
        dofConfig = chain.uniform("DepthOfFieldConfig", UNIFORM_BYTES);
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

        float totalWeight = 0f, focusDistance = 0f, aperture = 0f, blurRadius = 0f, strength = 0f, autoFocus = 0f;
        for (Weighted w : requests) {
            totalWeight += w.weight();
            focusDistance += w.weight() * w.params().focusDistance();
            aperture += w.weight() * w.params().aperture();
            blurRadius += w.weight() * w.params().blurRadius();
            strength += w.weight() * w.params().strength();
            autoFocus += w.weight() * (w.params().autoFocus() ? 1f : 0f);
        }
        requests.clear();
        if (totalWeight <= 0.0001f) return FrameContribution.NONE;

        float finalFocusDistance = focusDistance / totalWeight;
        float finalAperture = Math.max(aperture / totalWeight, 0.1f);
        float finalBlurRadius = blurRadius / totalWeight;
        float finalStrength = strength / totalWeight;
        float finalAutoFocus = (autoFocus / totalWeight) >= 0.5f ? 1f : 0f;

        if (!chain.prepare(frame)) return FrameContribution.NONE;

        // Mirrors Camera#update's own near/far computation — no post-chain
        // uniform exposes the camera's actual projection planes, so we
        // recompute them from the same player-facing settings.
        Minecraft mc = frame.mc();
        float renderDistanceBlocks = mc.options.getEffectiveRenderDistance() * 16.0f;
        float far = Math.max(renderDistanceBlocks * 4.0f, mc.options.cloudRange().get() * 16.0f);
        float near = Camera.PROJECTION_Z_NEAR;

        hRadius.write(builder -> builder.putFloat(finalBlurRadius));
        vRadius.write(builder -> builder.putFloat(finalBlurRadius));
        dofConfig.write(builder -> builder.putFloat(near).putFloat(far)
                .putFloat(finalFocusDistance).putFloat(finalAperture).putFloat(finalStrength).putFloat(finalAutoFocus));
        chain.run(frame);
        return FrameContribution.RAN;
    }

    @Override
    public void close() {
        requests.clear();
    }
}
