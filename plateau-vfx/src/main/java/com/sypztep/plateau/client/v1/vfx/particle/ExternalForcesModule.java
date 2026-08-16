package com.sypztep.plateau.client.v1.vfx.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The set of {@link ForceField}s a {@link ParticleSystem} sums each tick.
 * Fields may be exclusively owned by one system or shared across several
 * (e.g. one "wind zone" instance referenced by every system in an area) —
 * this module just holds references, it doesn't own their lifecycle.
 */
@Environment(EnvType.CLIENT)
public final class ExternalForcesModule {
    private final List<ForceField> fields = new ArrayList<>();

    public ExternalForcesModule add(ForceField field) {
        fields.add(field);
        return this;
    }

    public ExternalForcesModule remove(ForceField field) {
        fields.remove(field);
        return this;
    }

    /** Sums every field's contribution into {@code outForce} (added in place). */
    public void accumulate(Vector3f pos, Vector3f vel, Vector3f outForce) {
        for (ForceField field : fields) {
            field.apply(pos, vel, outForce);
        }
    }
}
