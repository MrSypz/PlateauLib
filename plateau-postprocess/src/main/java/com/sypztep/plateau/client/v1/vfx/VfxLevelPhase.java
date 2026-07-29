package com.sypztep.plateau.client.v1.vfx;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Level-render phases a {@link VfxEffect} can declare for its
 * {@link VfxEffect#prepare} pass. Each maps 1:1 to a Fabric
 * {@code LevelRenderEvents} phase; the manager owns the event registration so
 * every effect lives on one scheduling axis (priority within a phase, phase
 * within the frame) instead of ad hoc per-mod event listeners.
 */
@Environment(EnvType.CLIENT)
public enum VfxLevelPhase {
    AFTER_SOLID_FEATURES,
    BEFORE_TRANSLUCENT_TERRAIN,
    AFTER_TRANSLUCENT_TERRAIN,
    AFTER_TRANSLUCENT_FEATURES
}
