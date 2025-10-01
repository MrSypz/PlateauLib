package com.sypztep.plateau.common.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class PlateauPlayerEntityEvents {
    public static final Event<DamageDealt> DAMAGE_DEALT = EventFactory.createArrayBacked(DamageDealt.class, (listeners) -> (entity, source, finalDamage) -> {
        for (DamageDealt listener : listeners) {
            listener.onDamageDealt(entity, source, finalDamage);
        }
    });

    @FunctionalInterface
    public interface DamageDealt {
        void onDamageDealt(LivingEntity entity, DamageSource source, float finalDamage);
    }
}