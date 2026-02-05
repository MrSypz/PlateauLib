package com.sypztep.plateau.common.api;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class PlateauDamageTypes {
    private PlateauDamageTypes() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static ResourceKey<DamageType> createKey(Identifier id) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, id);
    }

    public static ResourceKey<DamageType> createKey(String namespace, String path) {
        return createKey(Identifier.fromNamespaceAndPath(namespace, path));
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> key, @Nullable Entity source, @Nullable Entity attacker) {
        Holder.Reference<DamageType> holder = level.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(key);
        return new DamageSource(holder, source, attacker);
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> key, @Nullable Entity attacker) {
        Holder.Reference<DamageType> holder = level.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(key);
        return new DamageSource(holder, attacker);
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> key) {
        Holder.Reference<DamageType> holder = level.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(key);
        return new DamageSource(holder);
    }
    public static Holder.Reference<DamageType> getHolder(Level level, ResourceKey<DamageType> key) {
        return level.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(key);
    }
}