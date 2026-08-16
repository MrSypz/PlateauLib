package com.sypztep.plateau.client.v1.vfx.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector3f;

/** A constant force in one direction — wind, a conveyor draft, etc. */
@Environment(EnvType.CLIENT)
public final class DirectionalForceField implements ForceField {
    private final Vector3f direction;
    private final float strength;

    /** {@code direction} need not be normalized; the field bakes in its length times {@code strength}. */
    public DirectionalForceField(Vector3f direction, float strength) {
        this.direction = new Vector3f(direction).normalize();
        this.strength = strength;
    }

    @Override
    public void apply(Vector3f pos, Vector3f vel, Vector3f outForce) {
        outForce.add(direction.x() * strength, direction.y() * strength, direction.z() * strength);
    }
}
