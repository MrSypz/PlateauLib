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
 * Built-in film grain post effect (see {@link VfxEffects#filmGrain()}). Same
 * request/blend pattern as {@link VignetteEffect}. Noise is seeded from the
 * engine's own {@code GameTime} global (via {@code minecraft:globals.glsl}),
 * not a custom time uniform.
 */
@Environment(EnvType.CLIENT)
public final class FilmGrainEffect implements VfxEffect {
    private static final Identifier CHAIN_ID = Identifier.fromNamespaceAndPath("plateau-postprocess", "film_grain");
    private static final int UNIFORM_BYTES = 16; // std140: float Intensity, float Size

    private record Weighted(float weight, FilmGrainParams params) {}

    private final List<Weighted> requests = new ArrayList<>();

    private VfxPostChain chain;
    private ManagedUniform uniform;

    public void requestFrame(float weight, FilmGrainParams params) {
        if (weight <= 0f) return;
        requests.add(new Weighted(weight, params));
    }

    @Override
    public void init(VfxContext ctx) {
        chain = ctx.own(VfxPostChain.builder(CHAIN_ID).build());
        uniform = chain.uniform("FilmGrainConfig", UNIFORM_BYTES);
    }

    @Override
    public FrameContribution contribute(VfxFrame frame) {
        if (requests.isEmpty()) return FrameContribution.NONE;

        float totalWeight = 0f, intensity = 0f, size = 0f;
        for (Weighted w : requests) {
            totalWeight += w.weight();
            intensity += w.weight() * w.params().intensity();
            size += w.weight() * w.params().size();
        }
        requests.clear();
        if (totalWeight <= 0.0001f) return FrameContribution.NONE;

        float finalIntensity = intensity / totalWeight;
        float finalSize = size / totalWeight;

        if (!chain.prepare(frame)) return FrameContribution.NONE;
        uniform.write(builder -> builder.putFloat(finalIntensity).putFloat(finalSize));
        chain.run(frame);
        return FrameContribution.RAN;
    }

    @Override
    public void close() {
        requests.clear();
    }
}
