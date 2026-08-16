package com.sypztep.plateau.client.v1.vfx.effects;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Parameters for one {@link GlitchEffect#requestFrame} call. */
@Environment(EnvType.CLIENT)
public record GlitchParams(float intensity, float blockSize) {
    public static final GlitchParams DEFAULT = new GlitchParams(0.3f, 24.0f);
}
