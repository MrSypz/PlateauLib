package com.sypztep.plateau.client.v1.vfx.effects;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Parameters for one {@link VignetteEffect#requestFrame} call. When multiple
 * requests overlap in a frame, each field blends by weighted average.
 */
@Environment(EnvType.CLIENT)
public record VignetteParams(float intensity, float roundness, float colorR, float colorG, float colorB) {
    public static final VignetteParams DEFAULT = new VignetteParams(0.4f, 0.5f, 0f, 0f, 0f);
}
