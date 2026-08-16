package com.sypztep.plateau.client.v1.vfx.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;

import java.util.function.Supplier;

/**
 * Where a {@link ParticleSystem}'s position/velocity arrays live relative to.
 *
 * <ul>
 *   <li>{@link #world()} — absolute world coordinates from the moment of
 *       spawn. Particles detach from whatever spawned them; standard choice
 *       for debris/embers that shouldn't follow their emitter around.</li>
 *   <li>{@link #local(Supplier)} / {@link #custom(Supplier)} — arrays store
 *       positions relative to an anchor transform re-sampled every tick, so
 *       the whole particle cluster rigidly follows the anchor (e.g. an aura
 *       welded to a moving/rotating entity — the knella {@code
 *       FireballRenderer} case this module was built to replace). {@code
 *       local}/{@code custom} are the same mechanism under the hood — kept as
 *       two names only so call sites read as "attached to the thing that
 *       spawned this" vs. "attached to an arbitrary supplied transform";
 *       splitting them into different record types would be a distinction
 *       with no behavioral difference.</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public sealed interface SimulationSpace permits SimulationSpace.World, SimulationSpace.Anchored {

    record World() implements SimulationSpace {}

    record Anchored(Supplier<Matrix4f> anchor) implements SimulationSpace {}

    static SimulationSpace world() {
        return new World();
    }

    /** Particles follow {@code anchor}, re-sampled every {@link ParticleSystem#tick}. */
    static SimulationSpace local(Supplier<Matrix4f> anchor) {
        return new Anchored(anchor);
    }

    /** Identical mechanics to {@link #local}; a distinct name for a non-emitter anchor. */
    static SimulationSpace custom(Supplier<Matrix4f> anchor) {
        return new Anchored(anchor);
    }
}
