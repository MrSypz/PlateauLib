package com.sypztep.plateau.common.api.entity;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import java.util.*;

public final class PlateauAttributeRegistry {

    private static final Map<AttributeTarget, Set<Holder<Attribute>>> REGISTRY =
            new EnumMap<>(AttributeTarget.class);

    static {
        for (AttributeTarget target : AttributeTarget.values())
            REGISTRY.put(target, new LinkedHashSet<>());
    }

    private PlateauAttributeRegistry() {}

    public static Holder<Attribute> register(
            Identifier id,
            double fallback,
            double min,
            double max,
            AttributeTarget target
    ) {

        if (BuiltInRegistries.ATTRIBUTE.containsKey(id)) {
            throw new IllegalStateException("Attribute already registered: " + id);
        }

        Holder<Attribute> holder = Registry.registerForHolder(
                BuiltInRegistries.ATTRIBUTE,
                id,
                new RangedAttribute(
                        "attribute.name." + id.getPath(),
                        fallback,
                        min,
                        max
                ).setSyncable(true)
        );

        REGISTRY.get(target).add(holder);

        return holder;
    }

    public static void inject(AttributeSupplier.Builder builder, AttributeTarget target) {
        REGISTRY.get(target).forEach(builder::add);
    }

    public enum AttributeTarget {
        LIVING,
        PLAYER
    }
}