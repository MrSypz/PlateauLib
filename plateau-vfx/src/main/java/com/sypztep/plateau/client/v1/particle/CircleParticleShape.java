package com.sypztep.plateau.client.v1.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

/** Spawns on a circle in the local XZ plane, radiating outward horizontally at {@code speed}. */
@Environment(EnvType.CLIENT)
public final class CircleParticleShape implements ParticleShape {
    private final float radius;
    private final float speed;

    public CircleParticleShape(float radius, float speed) {
        this.radius = radius;
        this.speed = speed;
    }

    @Override
    public void spawn(RandomSource random, Vector3f outPosition, Vector3f outVelocity) {
        float angle = random.nextFloat() * 2f * (float) Math.PI;
        float dx = (float) Math.cos(angle);
        float dz = (float) Math.sin(angle);

        outPosition.set(dx * radius, 0f, dz * radius);
        outVelocity.set(dx * speed, 0f, dz * speed);
    }
}
