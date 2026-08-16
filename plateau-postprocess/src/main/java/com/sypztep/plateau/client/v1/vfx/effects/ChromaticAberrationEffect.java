package com.sypztep.plateau.client.v1.vfx.effects;

import com.sypztep.plateau.client.v1.vfx.FrameContribution;
import com.sypztep.plateau.client.v1.vfx.ManagedUniform;
import com.sypztep.plateau.client.v1.vfx.VfxContext;
import com.sypztep.plateau.client.v1.vfx.VfxEffect;
import com.sypztep.plateau.client.v1.vfx.VfxFrame;
import com.sypztep.plateau.client.v1.vfx.VfxPostChain;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in chromatic aberration post effect (see {@link VfxEffects#chromaticAberration()}).
 * Same request/blend pattern as {@link VignetteEffect} — see that class for
 * the reference explanation.
 */
@Environment(EnvType.CLIENT)
public final class ChromaticAberrationEffect implements VfxEffect {
    private static final Identifier CHAIN_ID = Identifier.fromNamespaceAndPath("plateau-postprocess", "chromatic_aberration");
    private static final int UNIFORM_BYTES = 16; // std140: float Strength, float CenterFalloff

    private record Weighted(float weight, ChromaticAberrationParams params) {}

    private final List<Weighted> requests = new ArrayList<>();

    private VfxPostChain chain;
    private ManagedUniform uniform;

    /** Request this effect be active this frame with the given blend weight and parameters. */
    public void requestFrame(float weight, ChromaticAberrationParams params) {
        if (weight <= 0f) return;
        requests.add(new Weighted(weight, params));
    }

    @Override
    public void init(VfxContext ctx) {
        chain = ctx.own(VfxPostChain.builder(CHAIN_ID).build());
        uniform = chain.uniform("ChromaticAberrationConfig", UNIFORM_BYTES);
    }

    @Override
    public FrameContribution contribute(VfxFrame frame) {
        if (requests.isEmpty()) return FrameContribution.NONE;

        float totalWeight = 0f, strength = 0f, centerFalloff = 0f;
        for (Weighted w : requests) {
            totalWeight += w.weight();
            strength += w.weight() * w.params().strength();
            centerFalloff += w.weight() * w.params().centerFalloff();
        }
        requests.clear();
        if (totalWeight <= 0.0001f) return FrameContribution.NONE;

        float finalStrength = strength / totalWeight;
        float finalCenterFalloff = centerFalloff / totalWeight;

        if (!chain.prepare(frame)) return FrameContribution.NONE;
        uniform.write(builder -> builder.putFloat(finalStrength).putFloat(finalCenterFalloff));
        chain.run(frame);
        return FrameContribution.RAN;
    }

    @Override
    public void close() {
        requests.clear();
    }
}
