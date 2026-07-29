package com.sypztep.plateau.client.v1.vfx;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Result of {@link VfxEffect#contribute} — whether the effect actually drew
 * anything this frame. Returning {@link #NONE} skips the effect with zero
 * consumer-side bookkeeping; the framework records a skip reason for
 * {@link VfxDiagnostics}.
 */
@Environment(EnvType.CLIENT)
public enum FrameContribution {
    NONE,
    RAN
}
