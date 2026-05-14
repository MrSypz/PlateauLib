package com.sypztep.plateau.client.v2.ui.interaction;

import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public final class DragSlot<PayloadValue> {
    private static @Nullable DragSlot<?> activeSource;

    private final BaseComponent<?> owner;
    private final Class<PayloadValue> type;
    private final DragSource<PayloadValue> source;
    private final DropTarget<PayloadValue> target;

    private Supplier<@Nullable PayloadValue> getter = () -> null;
    private Consumer<@Nullable PayloadValue> setter = ignored -> {};
    private boolean transferHandled;

    DragSlot(BaseComponent<?> owner, Class<PayloadValue> type) {
        this.owner = owner;
        this.type = type;
        this.source = DragDrop.source(owner, type)
                .payload(() -> getter.get())
                .onDragStart(ignored -> beginTransfer())
                .onDropAccepted(ignored -> cleanupAcceptedTransfer())
                .onDragEnd(ignored -> finishTransfer());
        this.target = DragDrop.target(owner, type)
                .onDrop((value, event) -> acceptDrop(value));
    }

    public DragSlot<PayloadValue> value(Supplier<@Nullable PayloadValue> getter, Consumer<@Nullable PayloadValue> setter) {
        this.getter = getter == null ? () -> null : getter;
        this.setter = setter == null ? ignored -> {} : setter;
        return this;
    }

    public DragSlot<PayloadValue> label(Function<PayloadValue, Component> labeler) {
        source.label(labeler);
        return this;
    }

    public DragSlot<PayloadValue> label(Component label) {
        source.label(label);
        return this;
    }

    public DragSlot<PayloadValue> preview(DragPreviewRenderer<PayloadValue> previewRenderer) {
        source.preview(previewRenderer);
        return this;
    }

    public DragSlot<PayloadValue> hint(DropHint<PayloadValue> hintRenderer) {
        target.hint(hintRenderer);
        return this;
    }

    public DragSlot<PayloadValue> accepts(Predicate<PayloadValue> accepts) {
        target.accepts(accepts);
        return this;
    }

    public DragSlot<PayloadValue> button(int button) {
        source.button(button);
        return this;
    }

    public DragSlot<PayloadValue> startDistance(double startDistance) {
        source.startDistance(startDistance);
        return this;
    }

    public DragSlot<PayloadValue> previewAnimation(float followSpeed, float appearSpeed) {
        source.previewAnimation(followSpeed, appearSpeed);
        return this;
    }

    public DragSlot<PayloadValue> hintAnimation(float speed) {
        target.hintAnimation(speed);
        return this;
    }

    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        return source.mouseClicked(event, doubleClick);
    }

    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        return source.mouseDragged(event, dragX, dragY);
    }

    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        return source.mouseReleased(event);
    }

    public void extractHint(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        target.extractHint(graphics, mouseX, mouseY, delta);
    }

    public @Nullable PayloadValue value() {
        return getter.get();
    }

    public void value(@Nullable PayloadValue value) {
        setter.accept(value);
    }

    public BaseComponent<?> owner() {
        return owner;
    }

    public Class<PayloadValue> type() {
        return type;
    }

    private void beginTransfer() {
        activeSource = this;
        transferHandled = false;
    }

    private void acceptDrop(PayloadValue value) {
        DragSlot<PayloadValue> sourceSlot = activeSourceSlot();
        if (sourceSlot == this) {
            transferHandled = true;
            return;
        }

        PayloadValue previousValue = getter.get();
        setter.accept(value);

        if (sourceSlot != null) {
            sourceSlot.setter.accept(previousValue);
            sourceSlot.transferHandled = true;
        }
    }

    private void cleanupAcceptedTransfer() {
        if (activeSource == this && !transferHandled) {
            setter.accept(null);
        }
    }

    private void finishTransfer() {
        if (activeSource == this) activeSource = null;
        transferHandled = false;
    }

    @SuppressWarnings("unchecked")
    private @Nullable DragSlot<PayloadValue> activeSourceSlot() {
        if (!(activeSource instanceof DragSlot<?> slot)) return null;
        if (slot.type != type) return null;
        return (DragSlot<PayloadValue>) slot;
    }
}
