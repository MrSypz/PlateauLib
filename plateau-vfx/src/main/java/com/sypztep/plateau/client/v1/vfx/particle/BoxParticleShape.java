package com.sypztep.plateau.client.v1.vfx.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

/** Spawns at a uniformly random point inside a box volume, with an isotropic random velocity. */
@Environment(EnvType.CLIENT)
public final class BoxParticleShape implements ParticleShape {
    private final Vector3f halfExtents;
    private final float speed;

    public BoxParticleShape(Vector3f halfExtents, float speed) {
        this.halfExtents = new Vector3f(halfExtents);
        this.speed = speed;
    }

    @Override
    public void spawn(RandomSource random, Vector3f outPosition, Vector3f outVelocity) {
        outPosition.set(
                (random.nextFloat() * 2f - 1f) * halfExtents.x(),
                (random.nextFloat() * 2f - 1f) * halfExtents.y(),
                (random.nextFloat() * 2f - 1f) * halfExtents.z());

        float theta = random.nextFloat() * (float) Math.PI;
        float phi = random.nextFloat() * 2f * (float) Math.PI;
        float sinTheta = (float) Math.sin(theta);
        outVelocity.set(
                sinTheta * (float) Math.cos(phi) * speed,
                (float) Math.cos(theta) * speed,
                sinTheta * (float) Math.sin(phi) * speed);
    }
}
