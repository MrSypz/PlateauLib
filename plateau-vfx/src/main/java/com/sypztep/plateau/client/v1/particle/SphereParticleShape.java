package com.sypztep.plateau.client.v1.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

/** Spawns on a sphere's surface, radiating outward at {@code speed}. */
@Environment(EnvType.CLIENT)
public final class SphereParticleShape implements ParticleShape {
    private final float radius;
    private final float speed;

    public SphereParticleShape(float radius, float speed) {
        this.radius = radius;
        this.speed = speed;
    }

    @Override
    public void spawn(RandomSource random, Vector3f outPosition, Vector3f outVelocity) {
        float theta = random.nextFloat() * (float) Math.PI;
        float phi = random.nextFloat() * 2f * (float) Math.PI;
        float sinTheta = (float) Math.sin(theta);

        float dx = sinTheta * (float) Math.cos(phi);
        float dy = (float) Math.cos(theta);
        float dz = sinTheta * (float) Math.sin(phi);

        outPosition.set(dx * radius, dy * radius, dz * radius);
        outVelocity.set(dx * speed, dy * speed, dz * speed);
    }
}
