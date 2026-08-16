package com.sypztep.plateau.client.v1.vfx.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

/**
 * Spawn-distribution module for a {@link ParticleSystem} — the direct
 * analogue of Photon's Shape module. Fills a spawn-local position (relative
 * to the emitter origin, before {@link SimulationSpace} is applied) and an
 * initial velocity for one new particle.
 *
 * <p>Called once per spawned particle — implementations must not allocate.
 */
@Environment(EnvType.CLIENT)
@FunctionalInterface
public interface ParticleShape {
    void spawn(RandomSource random, Vector3f outPosition, Vector3f outVelocity);
}
