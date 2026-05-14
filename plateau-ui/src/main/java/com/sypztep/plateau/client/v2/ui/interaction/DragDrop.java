package com.sypztep.plateau.client.v2.ui.interaction;

import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class DragDrop {
    private static @Nullable Session<?> active;
    private static @Nullable DropTarget<?> hoveredTarget;

    private DragDrop() {}

    public static <PayloadValue> DragSource<PayloadValue> source(BaseComponent<?> owner, Class<PayloadValue> type) {
        return new DragSource<>(owner, type);
    }

    public static <PayloadValue> DropTarget<PayloadValue> target(BaseComponent<?> owner, Class<PayloadValue> type) {
        return new DropTarget<>(owner, type);
    }

    public static boolean active() {
        return active != null;
    }

    public static <PayloadValue> @Nullable DragPayload<PayloadValue> payload(Class<PayloadValue> type) {
        if (active == null || !active.payload().matches(type)) return null;
        return new DragPayload<>(type, type.cast(active.payload().value()), active.payload().label());
    }

    static <PayloadValue> void start(DragSource<PayloadValue> source, DragPayload<PayloadValue> payload) {
        active = new Session<>(source, payload);
        hoveredTarget = null;
    }

    static boolean isDragging(DragSource<?> source) {
        return active != null && active.source() == source;
    }

    public static void beginFrame() {
        hoveredTarget = null;
    }

    static void register(DropTarget<?> target) {
        if (active != null) hoveredTarget = target;
    }

    public static void updateMouse() {
        // Intentionally empty: screen-space mouse is sampled when rendering and dropping.
    }

    /**
     * Releases the active drag session.
     *
     * @return {@code true} when a drag session consumed this release.
     */
    public static boolean release(MouseButtonEvent event) {
        if (active == null) return false;

        Session<?> session = active;
        if (hoveredTarget != null) hoveredTarget.drop(session.payload(), event);
        finish(session);
        return true;
    }

    public static void cancel() {
        if (active == null) return;
        finish(active);
    }

    public static void renderPreview(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (active == null) return;
        renderSessionPreview(graphics, active, mouseX, mouseY, delta);
    }

    static <PayloadValue> void defaultPreview(GuiGraphicsExtractor graphics, DragPayload<PayloadValue> payload, int mouseX, int mouseY, float delta) {
        UITheme theme = UITheme.current();
        Component label = payload.label();
        int textWidth = Minecraft.getInstance().font.width(label);
        int previewX = mouseX + 12;
        int previewY = mouseY + 12;
        int previewWidth = Math.max(28, textWidth + 12);
        int previewHeight = 18;

        graphics.fill(previewX + 1, previewY + 1, previewX + previewWidth + 1, previewY + previewHeight + 1, 0x66000000);
        graphics.fill(previewX, previewY, previewX + previewWidth, previewY + previewHeight, theme.panel().bgHover());
        graphics.outline(previewX, previewY, previewWidth, previewHeight, theme.panel().borderHover());
        if (textWidth > 0) {
            graphics.text(Minecraft.getInstance().font, label, previewX + 6, previewY + 5, theme.text().primary(), true);
        }
    }

    static <PayloadValue> void defaultDropHint(GuiGraphicsExtractor graphics, DropTarget<PayloadValue> target, DragPayload<PayloadValue> payload, int mouseX, int mouseY, float delta) {
        BaseComponent<?> owner = target.owner();
        graphics.outline(owner.x(), owner.y(), owner.width(), owner.height(), UITheme.current().panel().borderHover());
        graphics.fill(owner.x() + 1, owner.y() + 1, owner.x() + owner.width() - 1, owner.y() + owner.height() - 1, 0x1800FF66);
    }

    @SuppressWarnings("unchecked")
    private static <PayloadValue> void renderSessionPreview(GuiGraphicsExtractor graphics, Session<?> session, int mouseX, int mouseY, float delta) {
        Session<PayloadValue> typed = (Session<PayloadValue>) session;
        typed.source().renderPreview(graphics, typed.payload(), mouseX, mouseY, delta);
    }

    @SuppressWarnings("unchecked")
    private static <PayloadValue> void finish(Session<?> session) {
        Session<PayloadValue> typed = (Session<PayloadValue>) session;
        active = null;
        hoveredTarget = null;
        typed.source().finishDrag(typed.payload());
    }

    private record Session<PayloadValue>(DragSource<PayloadValue> source, DragPayload<PayloadValue> payload) {}
}
