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
 * Built-in vignette post effect (see {@link VfxEffects#vignette()}).
 *
 * <p>Consumers call {@link #requestFrame} every frame they want it active;
 * this is the reference implementation of PlateauLib's request/blend model —
 * overlapping requests within the same frame resolve to a single weighted
 * average of their parameters, matching {@link FrameContribution}'s
 * run-or-skip contract.
 */
@Environment(EnvType.CLIENT)
public final class VignetteEffect implements VfxEffect {
    private static final Identifier CHAIN_ID = Identifier.fromNamespaceAndPath("plateau-postprocess", "vignette");
    private static final int UNIFORM_BYTES = 32; // std140: float Intensity, float Roundness, vec3 Color

    private record Weighted(float weight, VignetteParams params) {}

    private final List<Weighted> requests = new ArrayList<>();

    private VfxPostChain chain;
    private ManagedUniform uniform;

    /** Request this effect be active this frame with the given blend weight and parameters. */
    public void requestFrame(float weight, VignetteParams params) {
        if (weight <= 0f) return;
        requests.add(new Weighted(weight, params));
    }

    @Override
    public void init(VfxContext ctx) {
        chain = ctx.own(VfxPostChain.builder(CHAIN_ID).build());
        uniform = chain.uniform("VignetteConfig", UNIFORM_BYTES);
    }

    @Override
    public FrameContribution contribute(VfxFrame frame) {
        if (requests.isEmpty()) return FrameContribution.NONE;

        float totalWeight = 0f, intensity = 0f, roundness = 0f, r = 0f, g = 0f, b = 0f;
        for (Weighted w : requests) {
            totalWeight += w.weight();
            intensity += w.weight() * w.params().intensity();
            roundness += w.weight() * w.params().roundness();
            r += w.weight() * w.params().colorR();
            g += w.weight() * w.params().colorG();
            b += w.weight() * w.params().colorB();
        }
        requests.clear();
        if (totalWeight <= 0.0001f) return FrameContribution.NONE;

        float finalIntensity = intensity / totalWeight;
        float finalRoundness = roundness / totalWeight;
        float finalR = r / totalWeight, finalG = g / totalWeight, finalB = b / totalWeight;

        if (!chain.prepare(frame)) return FrameContribution.NONE;
        uniform.write(builder -> builder.putFloat(finalIntensity).putFloat(finalRoundness).putVec3(finalR, finalG, finalB));
        chain.run(frame);
        return FrameContribution.RAN;
    }

    @Override
    public void close() {
        requests.clear();
    }
}
