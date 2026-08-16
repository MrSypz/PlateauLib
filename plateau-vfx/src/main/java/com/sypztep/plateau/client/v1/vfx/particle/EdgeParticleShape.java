package com.sypztep.plateau.client.v1.vfx.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

/** Spawns at a uniformly random point along a line segment, with an isotropic random velocity. */
@Environment(EnvType.CLIENT)
public final class EdgeParticleShape implements ParticleShape {
    private final Vector3f start;
    private final Vector3f end;
    private final float speed;

    public EdgeParticleShape(Vector3f start, Vector3f end, float speed) {
        this.start = new Vector3f(start);
        this.end = new Vector3f(end);
        this.speed = speed;
    }

    @Override
    public void spawn(RandomSource random, Vector3f outPosition, Vector3f outVelocity) {
        float t = random.nextFloat();
        outPosition.set(start).lerp(end, t);

        float theta = random.nextFloat() * (float) Math.PI;
        float phi = random.nextFloat() * 2f * (float) Math.PI;
        float sinTheta = (float) Math.sin(theta);
        outVelocity.set(
                sinTheta * (float) Math.cos(phi) * speed,
                (float) Math.cos(theta) * speed,
                sinTheta * (float) Math.sin(phi) * speed);
    }
}
