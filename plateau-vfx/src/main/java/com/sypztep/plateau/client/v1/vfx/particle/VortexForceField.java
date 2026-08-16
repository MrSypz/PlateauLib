package com.sypztep.plateau.client.v1.vfx.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector3f;

/** A tangential force spinning particles around an axis — a swirl/tornado. */
@Environment(EnvType.CLIENT)
public final class VortexForceField implements ForceField {
    private static final float MIN_RADIUS_SQ = 0.0001f;

    private final Vector3f axisPoint;
    private final Vector3f axisDir;
    private final float strength;

    public VortexForceField(Vector3f axisPoint, Vector3f axisDir, float strength) {
        this.axisPoint = new Vector3f(axisPoint);
        this.axisDir = new Vector3f(axisDir).normalize();
        this.strength = strength;
    }

    @Override
    public void apply(Vector3f pos, Vector3f vel, Vector3f outForce) {
        float toX = pos.x() - axisPoint.x();
        float toY = pos.y() - axisPoint.y();
        float toZ = pos.z() - axisPoint.z();

        // Strip the component parallel to the axis, leaving the radial vector.
        float along = toX * axisDir.x() + toY * axisDir.y() + toZ * axisDir.z();
        float radX = toX - along * axisDir.x();
        float radY = toY - along * axisDir.y();
        float radZ = toZ - along * axisDir.z();
        float radiusSq = radX * radX + radY * radY + radZ * radZ;
        if (radiusSq < MIN_RADIUS_SQ) return;

        // Tangent = axis × radial, normalized.
        float tanX = axisDir.y() * radZ - axisDir.z() * radY;
        float tanY = axisDir.z() * radX - axisDir.x() * radZ;
        float tanZ = axisDir.x() * radY - axisDir.y() * radX;
        float invRadius = 1.0f / (float) Math.sqrt(radiusSq);

        outForce.add(tanX * invRadius * strength, tanY * invRadius * strength, tanZ * invRadius * strength);
    }
}
