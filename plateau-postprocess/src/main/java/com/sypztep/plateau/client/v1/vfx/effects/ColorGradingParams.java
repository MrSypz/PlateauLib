package com.sypztep.plateau.client.v1.vfx.effects;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Parameters for one {@link ColorGradingEffect#requestFrame} call. Parametric
 * grade (saturation/contrast/brightness/temperature) — see
 * {@link ColorGradingEffect} for why this ships instead of a LUT sampler.
 */
@Environment(EnvType.CLIENT)
public record ColorGradingParams(float saturation, float contrast, float brightness, float temperature) {
    public static final ColorGradingParams DEFAULT = new ColorGradingParams(1.0f, 1.0f, 0.0f, 0.0f);
}
