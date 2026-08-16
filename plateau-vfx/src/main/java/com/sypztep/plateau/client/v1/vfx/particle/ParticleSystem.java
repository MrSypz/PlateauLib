package com.sypztep.plateau.client.v1.vfx.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * A consumer-owned particle simulation: a fixed-capacity struct-of-arrays of
 * particle state (position, velocity, age, mass), a {@link ParticleShape}
 * spawn distribution, an {@link ExternalForcesModule}, and a
 * {@link SimulationSpace}. Not a global engine/registry — the owning mod
 * creates one per effect (an aura, a burst, a trail source) and drives
 * {@link #tick} itself, the same way knella's {@code FireballRenderer}
 * already owns its geometry directly. Rendering is likewise the consumer's
 * job: read {@link #worldPosition}/{@link #ageFraction} per particle and draw
 * with whatever mesh (e.g. {@code PrimitiveMeshes}) or billboard fits.
 *
 * <p>Fixed capacity, no growth — pick the max concurrent particle count up
 * front like most particle systems do; {@link #spawn} silently stops once
 * full rather than reallocating mid-effect.
 */
@Environment(EnvType.CLIENT)
public final class ParticleSystem {
    private final int capacity;
    private final float[] posX, posY, posZ;
    private final float[] velX, velY, velZ;
    private final float[] age, lifetime, mass;
    private int count;

    private final SimulationSpace space;
    private final ParticleShape shape;
    private final ExternalForcesModule forces;
    private final RandomSource random;

    // Reused across spawn()/tick() calls — never allocate per particle.
    private final Vector3f scratchPos = new Vector3f();
    private final Vector3f scratchVel = new Vector3f();
    private final Vector3f scratchForce = new Vector3f();

    public ParticleSystem(int capacity, SimulationSpace space, ParticleShape shape,
                           ExternalForcesModule forces, RandomSource random) {
        this.capacity = capacity;
        this.posX = new float[capacity];
        this.posY = new float[capacity];
        this.posZ = new float[capacity];
        this.velX = new float[capacity];
        this.velY = new float[capacity];
        this.velZ = new float[capacity];
        this.age = new float[capacity];
        this.lifetime = new float[capacity];
        this.mass = new float[capacity];
        this.space = space;
        this.shape = shape;
        this.forces = forces;
        this.random = random;
    }

    /**
     * Spawns up to {@code amount} new particles (fewer if {@link #capacity}
     * is reached). {@code originTransform} is the emitter's current
     * world transform — baked into the spawned particles' position/velocity
     * once, immediately, when {@link #space} is {@link SimulationSpace#world()}
     * (detach-at-spawn semantics); ignored for {@link SimulationSpace#local}/
     * {@link SimulationSpace#custom} particles, since those are stored
     * relative to their anchor and re-resolved every read instead — still
     * required in the signature so call sites don't need to branch on which
     * space mode they're using.
     */
    public void spawn(Matrix4f originTransform, int amount, float lifetimeTicks, float particleMass) {
        boolean bakeWorld = space instanceof SimulationSpace.World;
        for (int i = 0; i < amount && count < capacity; i++) {
            shape.spawn(random, scratchPos, scratchVel);
            if (bakeWorld) {
                originTransform.transformPosition(scratchPos);
                originTransform.transformDirection(scratchVel);
            }

            int idx = count++;
            posX[idx] = scratchPos.x();
            posY[idx] = scratchPos.y();
            posZ[idx] = scratchPos.z();
            velX[idx] = scratchVel.x();
            velY[idx] = scratchVel.y();
            velZ[idx] = scratchVel.z();
            age[idx] = 0f;
            lifetime[idx] = lifetimeTicks;
            mass[idx] = particleMass;
        }
    }

    /** Integrates every alive particle by {@code deltaTicks} and swap-removes any that expired. */
    public void tick(float deltaTicks) {
        for (int i = count - 1; i >= 0; i--) {
            age[i] += deltaTicks;
            if (age[i] >= lifetime[i]) {
                removeSwap(i);
                continue;
            }

            scratchPos.set(posX[i], posY[i], posZ[i]);
            scratchVel.set(velX[i], velY[i], velZ[i]);
            scratchForce.set(0f, 0f, 0f);
            forces.accumulate(scratchPos, scratchVel, scratchForce);

            float invMass = 1.0f / mass[i];
            velX[i] += scratchForce.x() * invMass * deltaTicks;
            velY[i] += scratchForce.y() * invMass * deltaTicks;
            velZ[i] += scratchForce.z() * invMass * deltaTicks;

            posX[i] += velX[i] * deltaTicks;
            posY[i] += velY[i] * deltaTicks;
            posZ[i] += velZ[i] * deltaTicks;
        }
    }

    private void removeSwap(int index) {
        int last = --count;
        posX[index] = posX[last];
        posY[index] = posY[last];
        posZ[index] = posZ[last];
        velX[index] = velX[last];
        velY[index] = velY[last];
        velZ[index] = velZ[last];
        age[index] = age[last];
        lifetime[index] = lifetime[last];
        mass[index] = mass[last];
    }

    public int count() {
        return count;
    }

    public int capacity() {
        return capacity;
    }

    public float age(int i) {
        return age[i];
    }

    public float ageFraction(int i) {
        return age[i] / lifetime[i];
    }

    /**
     * Particle {@code i}'s current world-space position, written into
     * {@code out}. For {@link SimulationSpace#local}/{@link
     * SimulationSpace#custom} particles this re-samples the anchor transform
     * fresh every call, so a moving/rotating anchor carries every particle
     * rigidly with it.
     */
    public Vector3f worldPosition(int i, Vector3f out) {
        out.set(posX[i], posY[i], posZ[i]);
        if (space instanceof SimulationSpace.Anchored anchored) {
            anchored.anchor().get().transformPosition(out);
        }
        return out;
    }
}
