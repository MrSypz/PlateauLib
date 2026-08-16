package com.sypztep.plateau.client.v1.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector3f;

/**
 * Point attraction (or repulsion, with a negative {@code strength}) toward
 * {@code center}, falling off to zero at {@code falloffRadius}. Inverse-
 * square within that radius (clamped near the center to avoid a singularity),
 * zero beyond it — a "black hole" pull with a bounded range rather than
 * vanilla physics' infinite-range gravity.
 */
@Environment(EnvType.CLIENT)
public final class GravityForceField implements ForceField {
    private static final float MIN_DIST_SQ = 0.01f;

    private final Vector3f center;
    private final float strength;
    private final float falloffRadius;

    public GravityForceField(Vector3f center, float strength, float falloffRadius) {
        this.center = new Vector3f(center);
        this.strength = strength;
        this.falloffRadius = falloffRadius;
    }

    @Override
    public void apply(Vector3f pos, Vector3f vel, Vector3f outForce) {
        float dx = center.x() - pos.x();
        float dy = center.y() - pos.y();
        float dz = center.z() - pos.z();
        float distSq = dx * dx + dy * dy + dz * dz;
        if (distSq >= falloffRadius * falloffRadius) return;

        float invDist = 1.0f / (float) Math.sqrt(Math.max(distSq, MIN_DIST_SQ));
        float magnitude = strength / Math.max(distSq, MIN_DIST_SQ);
        outForce.add(dx * invDist * magnitude, dy * invDist * magnitude, dz * invDist * magnitude);
    }
}
