package com.sypztep.plateau.client.v1.vfx.effects;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Parameters for one {@link BlurEffect#requestFrame} call. */
@Environment(EnvType.CLIENT)
public record BlurParams(float radius) {
    public static final BlurParams DEFAULT = new BlurParams(2.0f);
}
