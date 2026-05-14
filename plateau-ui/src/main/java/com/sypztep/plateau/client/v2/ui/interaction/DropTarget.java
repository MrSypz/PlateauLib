package com.sypztep.plateau.client.v2.ui.interaction;

import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
public final class DropTarget<PayloadValue> {
    private final BaseComponent<?> owner;
    private final Class<PayloadValue> type;

    private Predicate<PayloadValue> accepts = ignored -> true;
    private BiConsumer<PayloadValue, MouseButtonEvent> onDrop = (ignored, event) -> {};
    private DropHint<PayloadValue> hintRenderer = DragDrop::defaultDropHint;
    private float hintSpeed = 0.45f;
    private float hintProgress;

    DropTarget(BaseComponent<?> owner, Class<PayloadValue> type) {
        this.owner = owner;
        this.type = type;
    }

    public DropTarget<PayloadValue> accepts(Predicate<PayloadValue> accepts) {
        this.accepts = accepts == null ? ignored -> true : accepts;
        return this;
    }

    public DropTarget<PayloadValue> onDrop(BiConsumer<PayloadValue, MouseButtonEvent> onDrop) {
        this.onDrop = onDrop == null ? (ignored, event) -> {} : onDrop;
        return this;
    }

    public DropTarget<PayloadValue> hint(DropHint<PayloadValue> hintRenderer) {
        this.hintRenderer = hintRenderer == null ? DragDrop::defaultDropHint : hintRenderer;
        return this;
    }

    public DropTarget<PayloadValue> hintAnimation(float speed) {
        this.hintSpeed = Math.max(0f, speed);
        return this;
    }

    public void extractHint(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        DragPayload<PayloadValue> payload = DragDrop.payload(type);
        if (payload == null) {
            hintProgress = 0f;
            return;
        }

        boolean hovered = owner.isMouseOver(mouseX, mouseY) && canAccept(payload);
        hintProgress = Mth.lerp(DragDrop.animationStep(hintSpeed, delta), hintProgress, hovered ? 1f : 0f);
        if (hintProgress <= 0.01f) return;

        if (hovered) DragDrop.register(this);
        hintRenderer.extractRenderState(graphics, this, payload, mouseX, mouseY, delta);
    }

    boolean canAccept(DragPayload<PayloadValue> payload) {
        return payload.value() != null && accepts.test(payload.value());
    }

    boolean drop(DragPayload<?> payload, MouseButtonEvent event) {
        if (!payload.matches(type)) return false;

        PayloadValue value = type.cast(payload.value());
        if (!accepts.test(value)) return false;

        onDrop.accept(value, event);
        return true;
    }

    public BaseComponent<?> owner() {
        return owner;
    }

    public Class<PayloadValue> type() {
        return type;
    }

    public float hintProgress() {
        return hintProgress;
    }
}
