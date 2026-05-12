package com.sypztep.plateau.client.v2.ui.widget;

import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.core.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Full-screen modal dialog overlay.
 *
 * Usage — add as the LAST child of your screen root (outside any ScrollContainer)
 * so it renders on top and is not clipped by scissor:
 *
 * <pre>{@code
 *   DialogComponent dialog = new DialogComponent()
 *       .title("Confirm")
 *       .content(Components.text("Are you sure?").sizing(Sizing.fill(), Sizing.content()))
 *       .contentHeight(16)
 *       .button("OK",     d -> { doThing(); d.close(); })
 *       .button("Cancel", d -> d.close());
 *
 *   // root must be a layout that keeps dialog at the same position as its siblings
 *   // (e.g. a StackLayout / overlay container), not a FlowLayout that stacks children.
 *   Components.button("Open", b -> dialog.open());
 * }</pre>
 *
 * The component uses {@link Sizing#fill()} by default and must be mounted to the
 * full viewport area so the backdrop and centering math are correct.
 */
@Environment(EnvType.CLIENT)
public class DialogComponent extends BaseContainerComponent {

    // ── State ─────────────────────────────────────────────────

    private Component title = Component.literal("Dialog");
    private @Nullable BaseComponent content;
    private final List<ButtonComponent> buttonWidgets = new ArrayList<>();

    private boolean open         = false;
    private float   openProgress = 0f;

    // ── Configuration ─────────────────────────────────────────

    private int     dialogWidth       = 220;
    private int contentHeight = 40;   // used only when content != null
    private boolean closeOnBackdrop   = true;

    // ── Layout constants ──────────────────────────────────────

    private static final int HEADER_H = 22;
    private static final int FOOTER_H = 30;
    private static final int PAD      = 10;
    private static final int BTN_H    = 20;
    private static final int BTN_GAP  = 4;

    public DialogComponent() {
        this.horizontalSizing = Sizing.fill();
        this.verticalSizing   = Sizing.fill();
    }

    // ── Fluent API ────────────────────────────────────────────

    public DialogComponent title(Component title)         { this.title = title;   return this; }
    public DialogComponent title(String title)            { return title(Component.literal(title)); }
    public DialogComponent dialogWidth(int w)             { this.dialogWidth = w; remount(); return this; }
    public DialogComponent contentHeight(int h)           { this.contentHeight = h;    remount(); return this; }
    public DialogComponent closeOnBackdrop(boolean v)     { this.closeOnBackdrop = v; return this; }

    public DialogComponent content(@Nullable BaseComponent c) {
        this.content = c;
        remount();
        return this;
    }

    public DialogComponent button(Component label, Consumer<DialogComponent> action) {
        ButtonComponent button = new ButtonComponent(label)
                .onClick(b -> action.accept(this));

        buttonWidgets.add(button);
        remount();
        return this;
    }

    public DialogComponent button(String label, Consumer<DialogComponent> action) {
        return button(Component.literal(label), action);
    }

    public DialogComponent open()   { open = true;  return this; }
    public DialogComponent close()  { open = false; return this; }
    public DialogComponent toggle() { open = !open; return this; }
    public boolean isOpen()         { return open; }

    // ── Layout ────────────────────────────────────────────────

    @Override
    protected void onMounted() {
        remount();
    }

    private void remount() {
        if (width == 0 || height == 0) return;

        int dw = Math.min(dialogWidth, width - PAD * 4);
        int dh = totalDialogHeight();
        int dx = x + (width  - dw) / 2;
        int dy = y + (height - dh) / 2;

        // Content
        if (content != null) {
            content.mount(dx + PAD, dy + HEADER_H + PAD, dw - PAD * 2, contentHeight);
        }

        // Position buttons evenly in footer
        int n = buttonWidgets.size();
        if (n > 0) {
            int totalGap = BTN_GAP * (n - 1);
            int btnW     = (dw - PAD * 2 - totalGap) / n;
            int btnY     = dy + dh - FOOTER_H + (FOOTER_H - BTN_H) / 2;
            int curX     = dx + PAD;
            for (ButtonComponent btn : buttonWidgets) {
                btn.mount(curX, btnY, btnW, BTN_H);
                curX += btnW + BTN_GAP;
            }
        }
    }

    private int totalDialogHeight() {
        int inner = (content != null) ? PAD * 2 + contentHeight : PAD;
        return HEADER_H + inner + FOOTER_H;
    }

    @Override
    public @NonNull List<BaseComponent> children() {
        List<BaseComponent> list = new ArrayList<>();
        if (content != null) list.add(content);
        list.addAll(buttonWidgets);
        return list;
    }

    // ── Input — modal: absorb all events while visible ────────

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // Consume the whole viewport while the dialog is open
        return open && openProgress > 0.01f
                && mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!open || openProgress < 0.01f) return false;

        // Buttons first
        for (ButtonComponent btn : buttonWidgets) {
            if (btn.mouseClicked(event, doubleClick)) return true;
        }

        // Content
        if (content instanceof BaseComponent bc && bc.mouseClicked(event, doubleClick)) return true;

        // Backdrop click — close if enabled and click was outside the box
        if (closeOnBackdrop && !isOverBox(event.x(), event.y())) {
            close();
        }

        return true; // always absorb when open
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        for (ButtonComponent btn : buttonWidgets) btn.mouseReleased(event);
        return false;
    }

    private boolean isOverBox(double mx, double my) {
        int dw = Math.min(dialogWidth, width - PAD * 4);
        int dh = totalDialogHeight();
        int dx = x + (width  - dw) / 2;
        int dy = y + (height - dh) / 2;
        return mx >= dx && mx < dx + dw && my >= dy && my < dy + dh;
    }

    // ── Rendering ─────────────────────────────────────────────

    @Override
    public void extract(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        openProgress = stepAnimation(openProgress, open, 0.4f, delta);
        if (openProgress < 0.01f) return;

        UITheme theme = UITheme.current();

        int dw = Math.min(dialogWidth, width - PAD * 4);
        int dh = totalDialogHeight();
        int cx = x + width  / 2;
        int cy = y + height / 2;
        int dx = cx - dw / 2;
        int dy = cy - dh / 2;

        // ── Backdrop ──
        int backdropAlpha = (int)(openProgress * 0.7f * 255f);
        g.fill(x, y, x + width, y + height, ARGB.color(backdropAlpha, 0, 0, 0));

        // ── Dialog box — scale from center ──
        float scale = lerp(easeOut(openProgress), 0.90f, 1.0f);
        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        g.pose().scale(scale, scale);
        g.pose().translate(-cx, -cy);

        // Background fill
        g.fill(dx + 2, dy + 2, dx + dw - 2, dy + dh - 2, theme.panel().bg());

        // Outer border
        g.outline(dx,     dy,     dw,     dh,     theme.panel().border());
        g.outline(dx + 1, dy + 1, dw - 2, dh - 2, theme.panel().border());

        // Header background
        g.fill(dx + 2, dy + 2, dx + dw - 2, dy + HEADER_H, theme.panel().headerBg());

        // Title — centred in header
        g.centeredText(font, title, cx, dy + (HEADER_H - font.lineHeight) / 2,
                ARGB.srgbLerp(openProgress, theme.text().secondary(), theme.text().primary()));

        // Divider under header
        g.fill(dx + 2, dy + HEADER_H, dx + dw - 2, dy + HEADER_H + 1, theme.panel().border());

        // Content
        if (content != null) content.extractRenderState(g, mouseX, mouseY, delta);

        // Divider above footer
        int footerDivY = dy + dh - FOOTER_H;
        g.fill(dx + 2, footerDivY, dx + dw - 2, footerDivY + 1, theme.panel().border());

        // Buttons
        for (ButtonComponent btn : buttonWidgets) {
            btn.extractRenderState(g, mouseX, mouseY, delta);
        }

        g.pose().popMatrix();
    }

    // ── Helpers ───────────────────────────────────────────────

    private static float lerp(float t, float from, float to) {
        return from + (to - from) * t;
    }

    /** Cubic ease-out so the open animation decelerates nicely. */
    private static float easeOut(float t) {
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    // ── Fluent overrides ──────────────────────────────────────

    @Override public DialogComponent padding(Insets padding)    { super.padding(padding); return this; }
    @Override public DialogComponent margins(Insets margins)    { super.margins(margins); return this; }
    @Override public DialogComponent surface(Surface surface)   { super.surface(surface); return this; }
    @Override public DialogComponent id(String id)              { super.id(id);           return this; }
    @Override public DialogComponent visible(boolean visible)   { super.visible(visible); return this; }
    @Override public DialogComponent sizing(Sizing h, Sizing v) { super.sizing(h, v);     return this; }
    @Override public DialogComponent sizing(Sizing both)        { super.sizing(both);     return this; }
}