package com.sypztep.plateau.client.v2.ui.interaction;

import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v1.ui.core.RenderHelper;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class DragDrop {
    private static final float DEFAULT_PREVIEW_SCALE_START = 0.88f;

    private static @Nullable Session<?> active;
    private static @Nullable DropTarget<?> hoveredTarget;

    private DragDrop() {}

    public static <PayloadValue> DragSource<PayloadValue> source(BaseComponent<?> owner, Class<PayloadValue> type) {
        return new DragSource<>(owner, type);
    }

    public static <PayloadValue> DropTarget<PayloadValue> target(BaseComponent<?> owner, Class<PayloadValue> type) {
        return new DropTarget<>(owner, type);
    }

    public static <PayloadValue> DragSlot<PayloadValue> slot(BaseComponent<?> owner, Class<PayloadValue> type) {
        return new DragSlot<>(owner, type);
    }

    public static boolean active() {
        return active != null;
    }

    public static <PayloadValue> @Nullable DragPayload<PayloadValue> payload(Class<PayloadValue> type) {
        if (active == null || !active.payload().matches(type)) return null;
        return new DragPayload<>(type, type.cast(active.payload().value()), active.payload().label());
    }

    static <PayloadValue> void start(DragSource<PayloadValue> source, DragPayload<PayloadValue> payload, float mouseX, float mouseY) {
        active = new Session<>(source, payload, mouseX, mouseY);
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

    /**
     * Releases the active drag session.
     *
     * @return {@code true} when a drag session consumed this release.
     */
    public static boolean release(MouseButtonEvent event) {
        if (active == null) return false;

        Session<?> session = active;
        boolean accepted = hoveredTarget != null && hoveredTarget.drop(session.payload(), event);
        finish(session, accepted);
        return true;
    }

    public static void cancel() {
        if (active == null) return;
        finish(active, false);
    }

    public static void renderPreview(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (active == null) return;
        renderSessionPreview(graphics, active, mouseX, mouseY, delta);
    }

    static <PayloadValue> void defaultPreview(GuiGraphicsExtractor graphics, DragPayload<PayloadValue> payload, int mouseX, int mouseY, float delta) {
        UITheme theme = UITheme.current();
        Component label = payload.label();
        float alpha = active == null ? 1f : active.alpha();
        float scale = active == null ? 1f : active.scale();
        int textWidth = Minecraft.getInstance().font.width(label);
        int previewX = mouseX + 12;
        int previewY = mouseY + 12;
        int previewWidth = Math.max(28, textWidth + 12);
        int previewHeight = 18;
        int centerX = previewX + previewWidth / 2;
        int centerY = previewY + previewHeight / 2;

        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-centerX, -centerY);

        RenderHelper.floatingPanel(graphics,
                previewX,
                previewY,
                previewWidth,
                previewHeight,
                ARGB.black(alpha * (0x66 / 255f)),
                ARGB.color(alpha, theme.panel().bgHover()),
                ARGB.color(alpha, theme.panel().borderHover()));
        if (textWidth > 0) {
            graphics.text(Minecraft.getInstance().font, label, previewX + 6, previewY + 5, ARGB.color(alpha, theme.text().primary()), true);
        }
        graphics.pose().popMatrix();
    }

    static <PayloadValue> void defaultDropHint(GuiGraphicsExtractor graphics, DropTarget<PayloadValue> target, DragPayload<PayloadValue> payload, int mouseX, int mouseY, float delta) {
        BaseComponent<?> owner = target.owner();
        float progress = easeOutCubic(target.hintProgress());
        int inset = Math.round(Mth.lerp(progress, 4f, 0f));
        int fillAlpha = Math.round(Mth.lerp(progress, 0f, 0x20));
        int borderAlpha = Math.round(Mth.lerp(progress, 0f, 255f));
        int hintWidth = Math.max(0, owner.width() - inset * 2);
        int hintHeight = Math.max(0, owner.height() - inset * 2);
        graphics.fill(owner.x() + 1 + inset, owner.y() + 1 + inset, owner.x() + owner.width() - 1 - inset, owner.y() + owner.height() - 1 - inset, ARGB.color(fillAlpha, 0, 255, 102));
        graphics.outline(owner.x() + inset, owner.y() + inset, hintWidth, hintHeight, ARGB.color(borderAlpha, UITheme.current().panel().borderHover()));
    }

    @SuppressWarnings("unchecked")
    private static <PayloadValue> void renderSessionPreview(GuiGraphicsExtractor graphics, Session<?> session, int mouseX, int mouseY, float delta) {
        Session<PayloadValue> typed = (Session<PayloadValue>) session;
        typed.update(mouseX, mouseY, delta);
        typed.source().renderPreview(graphics, typed.payload(), Mth.floor(typed.visualMouseX()), Mth.floor(typed.visualMouseY()), delta);
    }

    @SuppressWarnings("unchecked")
    private static <PayloadValue> void finish(Session<?> session, boolean accepted) {
        Session<PayloadValue> typed = (Session<PayloadValue>) session;
        active = null;
        hoveredTarget = null;
        typed.source().finishDrag(typed.payload(), accepted);
    }

    static float animationStep(float speed, float delta) {
        return Mth.clamp(speed * Math.max(0f, delta), 0f, 1f);
    }

    private static float easeOutCubic(float progress) {
        float inverse = 1f - Mth.clamp(progress, 0f, 1f);
        return 1f - inverse * inverse * inverse;
    }

    private static final class Session<PayloadValue> {
        private final DragSource<PayloadValue> source;
        private final DragPayload<PayloadValue> payload;
        private float visualMouseX;
        private float visualMouseY;
        private float alpha;
        private float scaleProgress;

        private Session(DragSource<PayloadValue> source, DragPayload<PayloadValue> payload, float mouseX, float mouseY) {
            this.source = source;
            this.payload = payload;
            this.visualMouseX = mouseX;
            this.visualMouseY = mouseY;
        }

        private void update(float mouseX, float mouseY, float delta) {
            float follow = animationStep(source.previewFollowSpeed(), delta);
            float appear = animationStep(source.previewAppearSpeed(), delta);
            visualMouseX = Mth.lerp(follow, visualMouseX, mouseX);
            visualMouseY = Mth.lerp(follow, visualMouseY, mouseY);
            alpha = Mth.lerp(appear, alpha, 1f);
            scaleProgress = Mth.lerp(appear, scaleProgress, 1f);
        }

        private DragSource<PayloadValue> source() {
            return source;
        }

        private DragPayload<PayloadValue> payload() {
            return payload;
        }

        private float visualMouseX() {
            return visualMouseX;
        }

        private float visualMouseY() {
            return visualMouseY;
        }

        private float alpha() {
            return easeOutCubic(alpha);
        }

        private float scale() {
            return Mth.lerp(easeOutCubic(scaleProgress), DEFAULT_PREVIEW_SCALE_START, 1f);
        }
    }
}
