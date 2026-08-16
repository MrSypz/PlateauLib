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
 * Built-in color grading post effect (see {@link VfxEffects#colorGrading()}).
 *
 * <p><b>Parametric, not LUT-based.</b> {@code VfxPostChain}'s external-target
 * mechanism only wires up {@code RenderTarget}s (see its class doc) — there's
 * no existing plumbing to bind an arbitrary static texture (a LUT PNG) as a
 * post-chain sampler input. Adding that is a real framework extension, not a
 * one-shader task, so this v1 ships a saturation/contrast/brightness/
 * temperature grade instead, which covers the common case with zero new
 * framework surface. A LUT-based variant can follow once that capability
 * exists.
 */
@Environment(EnvType.CLIENT)
public final class ColorGradingEffect implements VfxEffect {
    private static final Identifier CHAIN_ID = Identifier.fromNamespaceAndPath("plateau-postprocess", "color_grading");
    private static final int UNIFORM_BYTES = 16; // std140: float Saturation, Contrast, Brightness, Temperature

    private record Weighted(float weight, ColorGradingParams params) {}

    private final List<Weighted> requests = new ArrayList<>();

    private VfxPostChain chain;
    private ManagedUniform uniform;

    public void requestFrame(float weight, ColorGradingParams params) {
        if (weight <= 0f) return;
        requests.add(new Weighted(weight, params));
    }

    @Override
    public void init(VfxContext ctx) {
        chain = ctx.own(VfxPostChain.builder(CHAIN_ID).build());
        uniform = chain.uniform("ColorGradingConfig", UNIFORM_BYTES);
    }

    @Override
    public FrameContribution contribute(VfxFrame frame) {
        if (requests.isEmpty()) return FrameContribution.NONE;

        float totalWeight = 0f, saturation = 0f, contrast = 0f, brightness = 0f, temperature = 0f;
        for (Weighted w : requests) {
            totalWeight += w.weight();
            saturation += w.weight() * w.params().saturation();
            contrast += w.weight() * w.params().contrast();
            brightness += w.weight() * w.params().brightness();
            temperature += w.weight() * w.params().temperature();
        }
        requests.clear();
        if (totalWeight <= 0.0001f) return FrameContribution.NONE;

        float finalSaturation = saturation / totalWeight;
        float finalContrast = contrast / totalWeight;
        float finalBrightness = brightness / totalWeight;
        float finalTemperature = temperature / totalWeight;

        if (!chain.prepare(frame)) return FrameContribution.NONE;
        uniform.write(builder -> builder.putFloat(finalSaturation).putFloat(finalContrast)
                .putFloat(finalBrightness).putFloat(finalTemperature));
        chain.run(frame);
        return FrameContribution.RAN;
    }

    @Override
    public void close() {
        requests.clear();
    }
}
