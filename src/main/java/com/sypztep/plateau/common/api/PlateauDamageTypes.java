package com.sypztep.plateau.common.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

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

    public static void hurt(Entity entity, ResourceKey<DamageType> key, float amount) {
        if (entity.level() instanceof ServerLevel serverLevel)
            entity.hurtServer(serverLevel, entity.damageSources().source(key), amount);
    }

    public static void hurt(Entity entity, ResourceKey<DamageType> key, float amount, @Nullable Entity attacker) {
        if (entity.level() instanceof ServerLevel serverLevel)
            entity.hurtServer(serverLevel, entity.damageSources().source(key, attacker), amount);
    }

    public static void hurt(Entity entity, ResourceKey<DamageType> key, float amount, @Nullable Entity direct, @Nullable Entity attacker) {
        if (entity.level() instanceof ServerLevel serverLevel)
            entity.hurtServer(serverLevel, entity.damageSources().source(key, direct, attacker), amount);
    }
}