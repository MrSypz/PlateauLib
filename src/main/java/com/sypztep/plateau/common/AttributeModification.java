package com.sypztep.plateau.common;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.function.ToDoubleFunction;

public record AttributeModification(Holder<Attribute> attribute, Identifier modifierId,
                                    AttributeModifier.Operation operation,
                                    ToDoubleFunction<Double> effectFunction,
                                    boolean displayAsPercent) {
    public AttributeModification(Holder<Attribute> attribute, Identifier modifierId,
                                 AttributeModifier.Operation operation,
                                 ToDoubleFunction<Double> effectFunction) {
        this(attribute, modifierId, operation, effectFunction, false);
    }
    public static AttributeModification addValue(Holder<Attribute> attribute,
                                                 Identifier modifierId,
                                                 ToDoubleFunction<Double> effectFunction) {
        return new AttributeModification(attribute, modifierId, AttributeModifier.Operation.ADD_VALUE, effectFunction, false);
    }
    public static AttributeModification addValueAsPercent(Holder<Attribute> attribute,
                                                          Identifier modifierId,
                                                          ToDoubleFunction<Double> effectFunction) {
        return new AttributeModification(attribute, modifierId, AttributeModifier.Operation.ADD_VALUE, effectFunction, true);
    }
    public static AttributeModification addMultiply(Holder<Attribute> attribute,
                                                    Identifier modifierId,
                                                    ToDoubleFunction<Double> effectFunction) {
        return new AttributeModification(attribute, modifierId, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, effectFunction, false);
    }
    public static AttributeModification addMultiplyTotal(Holder<Attribute> attribute,
                                                         Identifier modifierId,
                                                         ToDoubleFunction<Double> effectFunction) {
        return new AttributeModification(attribute, modifierId, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, effectFunction, false);
    }
}