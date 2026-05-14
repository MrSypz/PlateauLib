package com.sypztep.plateau.client.v2.ui.interaction;

import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public final class DragSource<PayloadValue> {
    private final BaseComponent<?> owner;
    private final Class<PayloadValue> type;

    private Supplier<@Nullable PayloadValue> payloadSupplier = () -> null;
    private Function<PayloadValue, Component> labeler = ignored -> Component.empty();
    private DragPreviewRenderer<PayloadValue> previewRenderer = DragDrop::defaultPreview;
    private Consumer<PayloadValue> onClick = ignored -> {};
    private Consumer<PayloadValue> onDragStart = ignored -> {};
    private Consumer<PayloadValue> onDragEnd = ignored -> {};
    private int button = 0;
    private double startDistance = 4.0;

    private @Nullable DragPayload<PayloadValue> pressedPayload;
    private double pressX;
    private double pressY;

    DragSource(BaseComponent<?> owner, Class<PayloadValue> type) {
        this.owner = owner;
        this.type = type;
    }

    public DragSource<PayloadValue> payload(Supplier<@Nullable PayloadValue> payloadSupplier) {
        this.payloadSupplier = payloadSupplier == null ? () -> null : payloadSupplier;
        return this;
    }

    public DragSource<PayloadValue> label(Function<PayloadValue, Component> labeler) {
        this.labeler = labeler == null ? ignored -> Component.empty() : labeler;
        return this;
    }

    public DragSource<PayloadValue> label(Component label) {
        this.labeler = ignored -> label == null ? Component.empty() : label;
        return this;
    }

    public DragSource<PayloadValue> preview(DragPreviewRenderer<PayloadValue> previewRenderer) {
        this.previewRenderer = previewRenderer == null ? DragDrop::defaultPreview : previewRenderer;
        return this;
    }

    public DragSource<PayloadValue> onClick(Consumer<PayloadValue> onClick) {
        this.onClick = onClick == null ? ignored -> {} : onClick;
        return this;
    }

    public DragSource<PayloadValue> onDragStart(Consumer<PayloadValue> onDragStart) {
        this.onDragStart = onDragStart == null ? ignored -> {} : onDragStart;
        return this;
    }

    public DragSource<PayloadValue> onDragEnd(Consumer<PayloadValue> onDragEnd) {
        this.onDragEnd = onDragEnd == null ? ignored -> {} : onDragEnd;
        return this;
    }

    public DragSource<PayloadValue> button(int button) {
        this.button = button;
        return this;
    }

    public DragSource<PayloadValue> startDistance(double startDistance) {
        this.startDistance = Math.max(0, startDistance);
        return this;
    }

    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != button || !owner.isMouseOver(event.x(), event.y())) return false;

        PayloadValue value = payloadSupplier.get();
        if (value == null) return false;

        pressedPayload = DragPayload.of(type, value, labeler.apply(value));
        pressX = event.x();
        pressY = event.y();

        onClick.accept(value);
        return true;
    }

    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (pressedPayload == null || event.button() != button) return false;

        if (!DragDrop.isDragging(this) && distanceFromPress(event) >= startDistance) {
            DragDrop.start(this, pressedPayload);
            onDragStart.accept(pressedPayload.value());
        }

        DragDrop.updateMouse();
        return true;
    }

    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (event.button() != button || pressedPayload == null) return false;

        if (DragDrop.isDragging(this)) {
            return DragDrop.release(event);
        } else {
            clearPress();
        }
        return true;
    }

    void clearPress() {
        pressedPayload = null;
    }

    void finishDrag(DragPayload<PayloadValue> payload) {
        onDragEnd.accept(payload.value());
        clearPress();
    }

    void renderPreview(GuiGraphicsExtractor graphics, DragPayload<PayloadValue> payload, int mouseX, int mouseY, float delta) {
        previewRenderer.render(graphics, payload, mouseX, mouseY, delta);
    }

    private double distanceFromPress(MouseButtonEvent event) {
        double dx = event.x() - pressX;
        double dy = event.y() - pressY;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
