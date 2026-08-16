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
 * Built-in bloom post effect (see {@link VfxEffects#bloom()}). v1 is a
 * single-scale bloom (threshold → one round of separable box blur →
 * additive composite), not a multi-mip downsample/upsample chain — simpler
 * and still visually correct, at the cost of the extra-soft falloff a mip
 * chain gives large light sources. Same request/blend pattern as
 * {@link VignetteEffect}.
 */
@Environment(EnvType.CLIENT)
public final class BloomEffect implements VfxEffect {
    private static final Identifier CHAIN_ID = Identifier.fromNamespaceAndPath("plateau-postprocess", "bloom");
    private static final int UNIFORM_BYTES = 16; // std140: one float each

    private record Weighted(float weight, BloomParams params) {}

    private final List<Weighted> requests = new ArrayList<>();

    private VfxPostChain chain;
    private ManagedUniform threshold;
    private ManagedUniform hRadius;
    private ManagedUniform vRadius;
    private ManagedUniform intensity;

    public void requestFrame(float weight, BloomParams params) {
        if (weight <= 0f) return;
        requests.add(new Weighted(weight, params));
    }

    @Override
    public void init(VfxContext ctx) {
        chain = ctx.own(VfxPostChain.builder(CHAIN_ID).build());
        threshold = chain.uniform("BloomThresholdConfig", UNIFORM_BYTES);
        hRadius = chain.uniform("BlurHConfig", UNIFORM_BYTES);
        vRadius = chain.uniform("BlurVConfig", UNIFORM_BYTES);
        intensity = chain.uniform("BloomCompositeConfig", UNIFORM_BYTES);
    }

    @Override
    public FrameContribution contribute(VfxFrame frame) {
        if (requests.isEmpty()) return FrameContribution.NONE;

        float totalWeight = 0f, thresholdSum = 0f, intensitySum = 0f, radiusSum = 0f;
        for (Weighted w : requests) {
            totalWeight += w.weight();
            thresholdSum += w.weight() * w.params().threshold();
            intensitySum += w.weight() * w.params().intensity();
            radiusSum += w.weight() * w.params().radius();
        }
        requests.clear();
        if (totalWeight <= 0.0001f) return FrameContribution.NONE;

        float finalThreshold = thresholdSum / totalWeight;
        float finalIntensity = intensitySum / totalWeight;
        float finalRadius = radiusSum / totalWeight;

        if (!chain.prepare(frame)) return FrameContribution.NONE;
        threshold.write(builder -> builder.putFloat(finalThreshold));
        hRadius.write(builder -> builder.putFloat(finalRadius));
        vRadius.write(builder -> builder.putFloat(finalRadius));
        intensity.write(builder -> builder.putFloat(finalIntensity));
        chain.run(frame);
        return FrameContribution.RAN;
    }

    @Override
    public void close() {
        requests.clear();
    }
}
