package com.sypztep.plateau.client.v2.ui.interaction;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record DragPayload<PayloadValue>(Class<PayloadValue> type, PayloadValue value, Component label) {
    public DragPayload {
        if (type == null) throw new IllegalArgumentException("Drag payload type cannot be null");
        if (label == null) label = Component.empty();
    }

    public static <PayloadValue> DragPayload<PayloadValue> of(Class<PayloadValue> type, PayloadValue value) {
        return new DragPayload<>(type, value, Component.empty());
    }

    public static <PayloadValue> DragPayload<PayloadValue> of(Class<PayloadValue> type, PayloadValue value, Component label) {
        return new DragPayload<>(type, value, label);
    }

    public boolean matches(Class<?> expectedType) {
        return expectedType != null && expectedType.isAssignableFrom(type);
    }

    public @Nullable PayloadValue nullableValue() {
        return value;
    }
}
