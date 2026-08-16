package com.sypztep.plateau.client.v1.vfx.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

/** Spawns at the apex (local origin), velocity randomized within a cone opening around +Y. */
@Environment(EnvType.CLIENT)
public final class ConeParticleShape implements ParticleShape {
    private final float angleRadians;
    private final float speed;

    public ConeParticleShape(float angleRadians, float speed) {
        this.angleRadians = angleRadians;
        this.speed = speed;
    }

    @Override
    public void spawn(RandomSource random, Vector3f outPosition, Vector3f outVelocity) {
        outPosition.set(0f, 0f, 0f);

        float theta = random.nextFloat() * angleRadians;
        float phi = random.nextFloat() * 2f * (float) Math.PI;
        float sinTheta = (float) Math.sin(theta);

        float dx = sinTheta * (float) Math.cos(phi);
        float dz = sinTheta * (float) Math.sin(phi);
        float dy = (float) Math.cos(theta);

        outVelocity.set(dx * speed, dy * speed, dz * speed);
    }
}
