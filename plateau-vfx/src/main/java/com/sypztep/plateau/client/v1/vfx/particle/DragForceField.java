package com.sypztep.plateau.client.v1.vfx.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector3f;

/** Opposes a particle's current velocity, linearly scaled by {@code coefficient} — simple air resistance. */
@Environment(EnvType.CLIENT)
public final class DragForceField implements ForceField {
    private final float coefficient;

    public DragForceField(float coefficient) {
        this.coefficient = coefficient;
    }

    @Override
    public void apply(Vector3f pos, Vector3f vel, Vector3f outForce) {
        outForce.add(-vel.x() * coefficient, -vel.y() * coefficient, -vel.z() * coefficient);
    }
}
