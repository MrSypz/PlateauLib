package com.sypztep.plateau.test;

import com.sypztep.plateau.common.v1.attribute.AttributeTarget;
import com.sypztep.plateau.common.v1.attribute.PlateauAttributeEntrypoint;
import com.sypztep.plateau.common.v1.attribute.PlateauAttributeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;

public final class TestAttributes implements PlateauAttributeEntrypoint {
    static final Holder<Attribute> TEST_LIVING = PlateauAttributeRegistry.register(
            Identifier.fromNamespaceAndPath("plateau-attributes-testmod", "test_living"),
            5.0, 0.0, 100.0, AttributeTarget.LIVING
    );
    static final Holder<Attribute> TEST_PLAYER = PlateauAttributeRegistry.register(
            Identifier.fromNamespaceAndPath("plateau-attributes-testmod", "test_player"),
            10.0, 0.0, 100.0, AttributeTarget.PLAYER
    );
}
