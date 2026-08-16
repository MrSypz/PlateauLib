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
 * Built-in full-screen blur (see {@link VfxEffects#blur()}) — a single round
 * of separable box blur (horizontal then vertical), reusing the same
 * {@code box_blur_h}/{@code box_blur_v} shaders {@link BloomEffect} and
 * {@link DepthOfFieldEffect} blur their intermediate targets with. Same
 * request/blend pattern as {@link VignetteEffect}.
 */
@Environment(EnvType.CLIENT)
public final class BlurEffect implements VfxEffect {
    private static final Identifier CHAIN_ID = Identifier.fromNamespaceAndPath("plateau-postprocess", "blur");
    private static final int UNIFORM_BYTES = 16; // std140: float Radius

    private record Weighted(float weight, BlurParams params) {}

    private final List<Weighted> requests = new ArrayList<>();

    private VfxPostChain chain;
    private ManagedUniform hRadius;
    private ManagedUniform vRadius;

    public void requestFrame(float weight, BlurParams params) {
        if (weight <= 0f) return;
        requests.add(new Weighted(weight, params));
    }

    @Override
    public void init(VfxContext ctx) {
        chain = ctx.own(VfxPostChain.builder(CHAIN_ID).build());
        hRadius = chain.uniform("BlurHConfig", UNIFORM_BYTES);
        vRadius = chain.uniform("BlurVConfig", UNIFORM_BYTES);
    }

    @Override
    public FrameContribution contribute(VfxFrame frame) {
        if (requests.isEmpty()) return FrameContribution.NONE;

        float totalWeight = 0f, radius = 0f;
        for (Weighted w : requests) {
            totalWeight += w.weight();
            radius += w.weight() * w.params().radius();
        }
        requests.clear();
        if (totalWeight <= 0.0001f) return FrameContribution.NONE;

        float finalRadius = radius / totalWeight;

        if (!chain.prepare(frame)) return FrameContribution.NONE;
        hRadius.write(builder -> builder.putFloat(finalRadius));
        vRadius.write(builder -> builder.putFloat(finalRadius));
        chain.run(frame);
        return FrameContribution.RAN;
    }

    @Override
    public void close() {
        requests.clear();
    }
}
