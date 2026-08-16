package com.sypztep.plateau.client.v1.vfx.effects;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Parameters for one {@link BloomEffect#requestFrame} call. */
@Environment(EnvType.CLIENT)
public record BloomParams(float threshold, float intensity, float radius) {
    public static final BloomParams DEFAULT = new BloomParams(0.8f, 1.0f, 4.0f);
}
