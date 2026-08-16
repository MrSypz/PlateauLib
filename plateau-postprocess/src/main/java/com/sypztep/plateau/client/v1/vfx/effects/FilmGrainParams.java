package com.sypztep.plateau.client.v1.vfx.effects;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Parameters for one {@link FilmGrainEffect#requestFrame} call. */
@Environment(EnvType.CLIENT)
public record FilmGrainParams(float intensity, float size) {
    public static final FilmGrainParams DEFAULT = new FilmGrainParams(0.05f, 1.0f);
}
