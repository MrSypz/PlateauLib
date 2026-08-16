package com.sypztep.plateau.client.v1.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector3f;

/**
 * A physical influence a {@link ParticleSystem} integrates each tick via its
 * {@link ExternalForcesModule}. Implementations add a **force** (not
 * acceleration — {@link ParticleSystem} divides by each particle's mass) into
 * {@code outForce}; they must add in place, never overwrite, since multiple
 * fields accumulate into the same vector for one particle per tick.
 *
 * <p>Called once per alive particle per tick — implementations must not
 * allocate.
 */
@Environment(EnvType.CLIENT)
@FunctionalInterface
public interface ForceField {
    void apply(Vector3f pos, Vector3f vel, Vector3f outForce);
}
