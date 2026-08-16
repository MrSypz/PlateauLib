package com.sypztep.plateau.client.v1.vfx.effects;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Parameters for one {@link ChromaticAberrationEffect#requestFrame} call.
 *
 * @param strength      overall channel-split distance
 * @param centerFalloff exponent shaping how quickly the split ramps up from
 *                      screen center to edge (higher = split stays near the edges)
 */
@Environment(EnvType.CLIENT)
public record ChromaticAberrationParams(float strength, float centerFalloff) {
    public static final ChromaticAberrationParams DEFAULT = new ChromaticAberrationParams(0.5f, 1.5f);
}
