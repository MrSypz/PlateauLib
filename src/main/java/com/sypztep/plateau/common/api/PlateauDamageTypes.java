package com.sypztep.plateau.common.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

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

    public static void hurt(Entity entity, DamageSource source, float amount) {
        if (entity.level() instanceof ServerLevel serverLevel)
            entity.hurtServer(serverLevel, source, amount);
    }
}