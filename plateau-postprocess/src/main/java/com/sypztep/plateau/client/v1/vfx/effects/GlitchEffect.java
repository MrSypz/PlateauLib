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
 * Built-in glitch post effect (see {@link VfxEffects#glitch()}). Block-shift
 * + channel split driven by the engine's {@code GameTime} global, same
 * request/blend pattern as {@link VignetteEffect}.
 */
@Environment(EnvType.CLIENT)
public final class GlitchEffect implements VfxEffect {
    private static final Identifier CHAIN_ID = Identifier.fromNamespaceAndPath("plateau-postprocess", "glitch");
    private static final int UNIFORM_BYTES = 16; // std140: float Intensity, float BlockSize

    private record Weighted(float weight, GlitchParams params) {}

    private final List<Weighted> requests = new ArrayList<>();

    private VfxPostChain chain;
    private ManagedUniform uniform;

    public void requestFrame(float weight, GlitchParams params) {
        if (weight <= 0f) return;
        requests.add(new Weighted(weight, params));
    }

    @Override
    public void init(VfxContext ctx) {
        chain = ctx.own(VfxPostChain.builder(CHAIN_ID).build());
        uniform = chain.uniform("GlitchConfig", UNIFORM_BYTES);
    }

    @Override
    public FrameContribution contribute(VfxFrame frame) {
        if (requests.isEmpty()) return FrameContribution.NONE;

        float totalWeight = 0f, intensity = 0f, blockSize = 0f;
        for (Weighted w : requests) {
            totalWeight += w.weight();
            intensity += w.weight() * w.params().intensity();
            blockSize += w.weight() * w.params().blockSize();
        }
        requests.clear();
        if (totalWeight <= 0.0001f) return FrameContribution.NONE;

        float finalIntensity = intensity / totalWeight;
        float finalBlockSize = blockSize / totalWeight;

        if (!chain.prepare(frame)) return FrameContribution.NONE;
        uniform.write(builder -> builder.putFloat(finalIntensity).putFloat(finalBlockSize));
        chain.run(frame);
        return FrameContribution.RAN;
    }

    @Override
    public void close() {
        requests.clear();
    }
}
