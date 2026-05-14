package com.sypztep.plateau.common.v1;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class TextParticleRegister {
    private TextParticleRegister() {}
    private static final Map<Identifier, TextParticleType> TYPES = new HashMap<>();

    public static TextParticleType registerType(TextParticleType type) {
        TYPES.put(type.id(), type);
        return type;
    }

    public static @Nullable TextParticleType getType(Identifier id) {
        return TYPES.get(id);
    }

    public static Map<Identifier, TextParticleType> getTypes() {
        return Collections.unmodifiableMap(TYPES);
    }
}
