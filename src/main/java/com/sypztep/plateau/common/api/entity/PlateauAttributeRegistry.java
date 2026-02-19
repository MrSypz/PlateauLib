package com.sypztep.plateau.common.api.entity;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class PlateauAttributeRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger("Plateau");

    private static boolean frozen = false;

    private static final Map<AttributeTarget, Set<Holder<Attribute>>> REGISTRY = new EnumMap<>(AttributeTarget.class);

    static {
        for (AttributeTarget target : AttributeTarget.values())
            REGISTRY.put(target, new LinkedHashSet<>());
    }

    private PlateauAttributeRegistry() {}

    public static void bootstrap() {
        if (frozen) return;

        long start = System.nanoTime();

        List<PlateauAttributeEntrypoint> entrypoints = FabricLoader.getInstance()
                .getEntrypoints("plateau_attributes", PlateauAttributeEntrypoint.class);

        entrypoints.forEach(PlateauAttributeEntrypoint::load);
        frozen = true;

        long elapsed = (System.nanoTime() - start) / 1_000_000;

        int living = REGISTRY.get(AttributeTarget.LIVING).size();
        int player = REGISTRY.get(AttributeTarget.PLAYER).size();
        int total  = living + player;

        LOGGER.info("Bootstrapped {} attribute(s) from {} entrypoint(s) ({} living, {} player-only) in {} ms",
                total, entrypoints.size(), living, player, elapsed);
    }

    public static Holder<Attribute> register(Identifier id, double fallback, double min, double max, AttributeTarget target) {
        if (frozen) throw new IllegalStateException("Cannot register attributes after bootstrap: " + id);

        if (BuiltInRegistries.ATTRIBUTE.containsKey(id))
            throw new IllegalStateException("Attribute already registered: " + id);

        Holder<Attribute> holder = Registry.registerForHolder(
                BuiltInRegistries.ATTRIBUTE, id,
                new RangedAttribute("attribute.name." + id.getPath(), fallback, min, max).setSyncable(true)
        );

        REGISTRY.get(target).add(holder);
        return holder;
    }

    /**
     * Injects attributes for the given target into the builder.
     * <p>
     * Safe to call on Player: LIVING attributes are already present from LivingEntity.createLivingAttributes(),
     * so only PLAYER-targeted attributes need to be added here. The builder itself guards against duplicates,
     * but we do a belt-and-suspenders check to avoid redundant work and noisy crashes.
     * </p>
     */
    public static void inject(AttributeSupplier.Builder builder, AttributeTarget target) {
        Set<Holder<Attribute>> toInject = REGISTRY.get(target);
        if (toInject.isEmpty()) return;

        // PLAYER target: skip any attribute that is already registered under LIVING
        // (shouldn't happen by design, but guards against accidental double-registration)
        Set<Holder<Attribute>> living = REGISTRY.get(AttributeTarget.LIVING);

        for (Holder<Attribute> attr : toInject) {
            if (target == AttributeTarget.PLAYER && living.contains(attr)) {
                LOGGER.warn("Skipping duplicate attribute '{}' already injected via LIVING target",
                        attr.unwrapKey().map(resourceKey -> resourceKey.identifier().toString()).orElse("unknown"));
                continue;
            }
            builder.add(attr);
        }
    }
}