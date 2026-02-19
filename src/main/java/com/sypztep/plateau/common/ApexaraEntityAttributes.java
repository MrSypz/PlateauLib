package com.sypztep.plateau.common;

import com.sypztep.plateau.Plateau;
import com.sypztep.plateau.common.api.entity.AttributeTarget;
import com.sypztep.plateau.common.api.entity.PlateauAttributeEntrypoint;
import com.sypztep.plateau.common.api.entity.PlateauAttributeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;

public final class ApexaraEntityAttributes implements PlateauAttributeEntrypoint {
    // --- Core Stats ---
    public static final Holder<Attribute> HEALTH_REGEN = register("health_regen", 0.0, 0.0, Double.MAX_VALUE, AttributeTarget.LIVING);
    public static final Holder<Attribute> PASSIVE_HEALTH_REGEN = register("passive_health_regen", 0.0, 0.0, Double.MAX_VALUE, AttributeTarget.PLAYER);

    public static final Holder<Attribute> RESOURCE = register("resource", 0, 0.0, 10000000, AttributeTarget.PLAYER);
    public static final Holder<Attribute> RESOURCE_REGEN = register("resource_regen", 1, 0.0, 10000000, AttributeTarget.PLAYER);
    public static final Holder<Attribute> PASSIVE_RESOURCE_REGEN = register("passive_resource_regen", 0, 0.0, 10000000, AttributeTarget.PLAYER);
    public static final Holder<Attribute> RESOURCE_REGEN_RATE = register("resource_regen_rate", 8, -1, 10000000, AttributeTarget.PLAYER);

    // --- Combat Stats ---
    public static final Holder<Attribute> ACCURACY = register("accuracy", 0, 0.0, 2048.0D, AttributeTarget.LIVING);
    public static final Holder<Attribute> EVASION = register("evasion", 0, 0.0, 2048.0D, AttributeTarget.LIVING);
    public static final Holder<Attribute> CRIT_DAMAGE = register("crit_damage", 1, 0.0, 10.24D, AttributeTarget.LIVING);
    public static final Holder<Attribute> CRIT_CHANCE = register("crit_chance", 0.0, 0.0, 2.0D, AttributeTarget.LIVING);
    public static final Holder<Attribute> BACK_ATTACK = register("back_attack", 0.2, 0.0, 10.24D, AttributeTarget.LIVING);
    public static final Holder<Attribute> SPECIAL_ATTACK = register("special_attack", 0.0, 0.0, 10.24D, AttributeTarget.LIVING);

    // --- Combat Type Damage (Melee, Magic, Projectile) ---
    public static final Holder<Attribute> MELEE_ATTACK_DAMAGE_FLAT = register("melee_attack_damage_flat", 1, 0.0, 4096, AttributeTarget.LIVING);
    public static final Holder<Attribute> MELEE_ATTACK_DAMAGE_MULT = register("melee_attack_damage_mult", 0, 0.0, 10.24D, AttributeTarget.LIVING);
    public static final Holder<Attribute> MAGIC_ATTACK_DAMAGE_FLAT = register("magic_attack_damage_flat", 1, 0.0, 4096, AttributeTarget.LIVING);
    public static final Holder<Attribute> MAGIC_ATTACK_DAMAGE_MULT = register("magic_attack_damage_mult", 0, 0.0, 10.24D, AttributeTarget.LIVING);
    public static final Holder<Attribute> PROJECTILE_ATTACK_DAMAGE_FLAT = register("projectile_attack_damage_flat", 1, 0.0, 4096, AttributeTarget.LIVING);
    public static final Holder<Attribute> PROJECTILE_ATTACK_DAMAGE_MULT = register("projectile_attack_damage_mult", 0, 0.0, 10.24D, AttributeTarget.LIVING);

    // --- Combat Type Defense ---
    public static final Holder<Attribute> PROJECTILE_RESISTANCE = register("projectile_resistance", 0, -10.0D, 0.75D, AttributeTarget.LIVING);
    public static final Holder<Attribute> MELEE_RESISTANCE = register("melee_resistance", 0, -10.0D, 0.75D, AttributeTarget.LIVING);
    public static final Holder<Attribute> MAGIC_RESISTANCE = register("magic_resistance", 0, -10.0D, 0.75D, AttributeTarget.LIVING);
    public static final Holder<Attribute> FLAT_PROJECTILE_REDUCTION = register("flat_projectile_reduction", 0, 0.0D, 20.0D, AttributeTarget.LIVING);
    public static final Holder<Attribute> FLAT_MELEE_REDUCTION = register("flat_melee_reduction", 0, 0.0D, 20.0D, AttributeTarget.LIVING);
    public static final Holder<Attribute> FLAT_MAGIC_REDUCTION = register("flat_magic_reduction", 0, 0.0D, 20.0D, AttributeTarget.LIVING);

    // --- Elemental Damage (Flat) ---
    public static final Holder<Attribute> NEUTRAL_ATTACK_DAMAGE_FLAT = register("neutral_attack_damage_flat", 0, 0.0, 4096, AttributeTarget.LIVING);
    public static final Holder<Attribute> FIRE_ATTACK_DAMAGE_FLAT = register("fire_attack_damage_flat", 0, 0.0, 4096, AttributeTarget.LIVING);
    public static final Holder<Attribute> COLD_ATTACK_DAMAGE_FLAT = register("cold_attack_damage_flat", 0, 0.0, 4096, AttributeTarget.LIVING);
    public static final Holder<Attribute> ELECTRIC_ATTACK_DAMAGE_FLAT = register("electric_attack_damage_flat", 0, 0.0, 4096, AttributeTarget.LIVING);
    public static final Holder<Attribute> WATER_ATTACK_DAMAGE_FLAT = register("water_attack_damage_flat", 0, 0.0, 4096, AttributeTarget.LIVING);
    public static final Holder<Attribute> WIND_ATTACK_DAMAGE_FLAT = register("wind_attack_damage_flat", 0, 0.0, 4096, AttributeTarget.LIVING);
    public static final Holder<Attribute> HOLY_ATTACK_DAMAGE_FLAT = register("holy_attack_damage_flat", 0, 0.0, 4096, AttributeTarget.LIVING);
    public static final Holder<Attribute> EARTH_ATTACK_DAMAGE_FLAT = register("earth_attack_damage_flat", 0, 0.0, 4096, AttributeTarget.LIVING);
    public static final Holder<Attribute> POISON_ATTACK_DAMAGE_FLAT = register("poison_attack_damage_flat", 0, 0.0, 4096, AttributeTarget.LIVING);
    public static final Holder<Attribute> SHADOW_ATTACK_DAMAGE_FLAT = register("shadow_attack_damage_flat", 0, 0.0, 4096, AttributeTarget.LIVING);
    public static final Holder<Attribute> GHOST_ATTACK_DAMAGE_FLAT = register("ghost_attack_damage_flat", 0, 0.0, 4096, AttributeTarget.LIVING);
    public static final Holder<Attribute> UNDEAD_ATTACK_DAMAGE_FLAT = register("undead_attack_damage_flat", 0, 0.0, 4096, AttributeTarget.LIVING);

    // --- Elemental Multipliers (Affinity) ---
    public static final Holder<Attribute> NEUTRAL_ATTACK_DAMAGE_MULT = register("neutral_attack_damage_mult", 0, 0.0, 10.24D, AttributeTarget.LIVING);
    public static final Holder<Attribute> FIRE_ATTACK_DAMAGE_MULT = register("fire_attack_damage_mult", 0, 0.0, 10.24D, AttributeTarget.LIVING);
    public static final Holder<Attribute> COLD_ATTACK_DAMAGE_MULT = register("cold_attack_damage_mult", 0, 0.0, 10.24D, AttributeTarget.LIVING);
    public static final Holder<Attribute> ELECTRIC_ATTACK_DAMAGE_MULT = register("electric_attack_damage_mult", 0, 0.0, 10.24D, AttributeTarget.LIVING);
    public static final Holder<Attribute> WATER_ATTACK_DAMAGE_MULT = register("water_attack_damage_mult", 0, 0.0, 10.24D, AttributeTarget.LIVING);
    public static final Holder<Attribute> WIND_ATTACK_DAMAGE_MULT = register("wind_attack_damage_mult", 0, 0.0, 10.24D, AttributeTarget.LIVING);
    public static final Holder<Attribute> HOLY_ATTACK_DAMAGE_MULT = register("holy_attack_damage_mult", 0, 0.0, 10.24D, AttributeTarget.LIVING);
    public static final Holder<Attribute> EARTH_ATTACK_DAMAGE_MULT = register("earth_attack_damage_mult", 0, 0.0, 10.24D, AttributeTarget.LIVING);
    public static final Holder<Attribute> POISON_ATTACK_DAMAGE_MULT = register("poison_attack_damage_mult", 0, 0.0, 10.24D, AttributeTarget.LIVING);
    public static final Holder<Attribute> SHADOW_ATTACK_DAMAGE_MULT = register("shadow_attack_damage_mult", 0, 0.0, 10.24D, AttributeTarget.LIVING);
    public static final Holder<Attribute> GHOST_ATTACK_DAMAGE_MULT = register("ghost_attack_damage_mult", 0, 0.0, 10.24D, AttributeTarget.LIVING);
    public static final Holder<Attribute> UNDEAD_ATTACK_DAMAGE_MULT = register("undead_attack_damage_mult", 0, 0.0, 10.24D, AttributeTarget.LIVING);

    // --- Elemental Resistance ---
    public static final Holder<Attribute> NEUTRAL_RESISTANCE = register("neutral_resistance", 0, -10.0D, 0.75D, AttributeTarget.LIVING);
    public static final Holder<Attribute> FIRE_RESISTANCE = register("fire_resistance", 0, -10.0D, 0.75D, AttributeTarget.LIVING);
    public static final Holder<Attribute> ELECTRIC_RESISTANCE = register("electric_resistance", 0, -10.0D, 0.75D, AttributeTarget.LIVING);
    public static final Holder<Attribute> WATER_RESISTANCE = register("water_resistance", 0, -10.0D, 0.75D, AttributeTarget.LIVING);
    public static final Holder<Attribute> WIND_RESISTANCE = register("wind_resistance", 0, -10.0D, 0.75D, AttributeTarget.LIVING);
    public static final Holder<Attribute> HOLY_RESISTANCE = register("holy_resistance", 0, -10.0D, 0.75D, AttributeTarget.LIVING);
    public static final Holder<Attribute> COLD_RESISTANCE = register("cold_resistance", 0, -10.0D, 0.75D, AttributeTarget.LIVING);
    public static final Holder<Attribute> EARTH_RESISTANCE = register("earth_resistance", 0, -10.0D, 0.75D, AttributeTarget.LIVING);
    public static final Holder<Attribute> POISON_RESISTANCE = register("poison_resistance", 0, -10.0D, 0.75D, AttributeTarget.LIVING);
    public static final Holder<Attribute> SHADOW_RESISTANCE = register("shadow_resistance", 0, -10.0D, 0.75D, AttributeTarget.LIVING);
    public static final Holder<Attribute> GHOST_RESISTANCE = register("ghost_resistance", 0, -10.0D, 0.75D, AttributeTarget.LIVING);
    public static final Holder<Attribute> UNDEAD_RESISTANCE = register("undead_resistance", 0, -10.0D, 0.75D, AttributeTarget.LIVING);

    // --- Elemental Flat Reduction ---
    public static final Holder<Attribute> FLAT_NEUTRAL_REDUCTION = register("flat_neutral_reduction", 0, 0.0D, 20.0D, AttributeTarget.LIVING);
    public static final Holder<Attribute> FLAT_FIRE_REDUCTION = register("flat_fire_reduction", 0, 0.0D, 20.0D, AttributeTarget.LIVING);
    public static final Holder<Attribute> FLAT_ELECTRIC_REDUCTION = register("flat_electric_reduction", 0, 0.0D, 20.0D, AttributeTarget.LIVING);
    public static final Holder<Attribute> FLAT_WATER_REDUCTION = register("flat_water_reduction", 0, 0.0D, 20.0D, AttributeTarget.LIVING);
    public static final Holder<Attribute> FLAT_WIND_REDUCTION = register("flat_wind_reduction", 0, 0.0D, 20.0D, AttributeTarget.LIVING);
    public static final Holder<Attribute> FLAT_HOLY_REDUCTION = register("flat_holy_reduction", 0, 0.0D, 20.0D, AttributeTarget.LIVING);
    public static final Holder<Attribute> FLAT_COLD_REDUCTION = register("flat_cold_reduction", 0, 0.0D, 20.0D, AttributeTarget.LIVING);
    public static final Holder<Attribute> FLAT_EARTH_REDUCTION = register("flat_earth_reduction", 0, 0.0D, 20.0D, AttributeTarget.LIVING);
    public static final Holder<Attribute> FLAT_POISON_REDUCTION = register("flat_poison_reduction", 0, 0.0D, 20.0D, AttributeTarget.LIVING);
    public static final Holder<Attribute> FLAT_SHADOW_REDUCTION = register("flat_shadow_reduction", 0, 0.0D, 20.0D, AttributeTarget.LIVING);
    public static final Holder<Attribute> FLAT_GHOST_REDUCTION = register("flat_ghost_reduction", 0, 0.0D, 20.0D, AttributeTarget.LIVING);
    public static final Holder<Attribute> FLAT_UNDEAD_REDUCTION = register("flat_undead_reduction", 0, 0.0D, 20.0D, AttributeTarget.LIVING);

    // --- Progression & Class Stats ---
    public static final Holder<Attribute> SPELL_POWER = register("spell_power", 0, 0.0, Double.MAX_VALUE, AttributeTarget.PLAYER);
    public static final Holder<Attribute> SWORD_MASTERY = register("sword_mastery", 0, 0.0, 10.24D, AttributeTarget.PLAYER);
    public static final Holder<Attribute> TWO_HAND_SWORD_MASTERY = register("two_hand_sword_mastery", 0, 0.0, 10.24D, AttributeTarget.PLAYER);
    public static final Holder<Attribute> SPEAR_MASTERY = register("spear_mastery", 0, 0.0, 10.24D, AttributeTarget.PLAYER);
    public static final Holder<Attribute> MAX_WEIGHT = register("max_weight", 400, 0D, 1000000, AttributeTarget.PLAYER);
    public static final Holder<Attribute> HEAL_EFFECTIVE = register("heal_effective", 0, -10.0D, 10.24D, AttributeTarget.LIVING);

    private static Holder<Attribute> register(String name, double fallback, double min, double max, AttributeTarget target) {
        return PlateauAttributeRegistry.register(Plateau.id(name), fallback, min, max, target);
    }
    @Override
    public void load() {

    }
}