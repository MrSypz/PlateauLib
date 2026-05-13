package com.sypztep.plateau.client.v2.ui.overlay;

import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.core.*;
import com.sypztep.plateau.client.v2.ui.widget.ButtonComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

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
 *       .dialogHeight(16)
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
public class DialogComponent extends BaseContainerComponent<DialogComponent> {

    // ── State ─────────────────────────────────────────────────

    private Component title = Component.literal("Dialog");
    private @Nullable BaseComponent<?> content;
    private final List<ButtonComponent> buttonWidgets = new ArrayList<>();

    private boolean open         = false;
    private float   openProgress = 0f;

    // ── Configuration ─────────────────────────────────────────

    private int dialogWidth = 220;
    private int dialogHeight = 40; // used only when content != null
    private boolean closeOnBackdrop = true;

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

    public DialogComponent title(Component title)               { this.title = title; return this; }
    public DialogComponent title(String title)                  { return title(Component.literal(title)); }
    public DialogComponent dialogWidth(int width)               { this.dialogWidth = Math.max(0, width); remount(); return this; }
    public DialogComponent dialogHeight(int height)             { this.dialogHeight = Math.max(0, height); remount(); return this; }
    public DialogComponent closeOnBackdrop(boolean onClose)     { this.closeOnBackdrop = onClose; return this; }
    public DialogComponent content(@Nullable BaseComponent<?> contentComponent) {
        this.content = contentComponent;
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

        int dialogBoxWidth = dialogWidth();
        int dialogBoxHeight = totalDialogHeight();
        int dialogX = x + (width - dialogBoxWidth) / 2;
        int dialogY = y + (height - dialogBoxHeight) / 2;

        // Content
        if (content != null) {
            content.mount(dialogX + PAD, dialogY + HEADER_H + PAD, dialogBoxWidth - PAD * 2, dialogHeight);
        }

        // Position buttons evenly in footer
        int buttonCount = buttonWidgets.size();
        if (buttonCount > 0) {
            int totalGap = BTN_GAP * (buttonCount - 1);
            int buttonWidth = (dialogBoxWidth - PAD * 2 - totalGap) / buttonCount;
            int buttonY = dialogY + dialogBoxHeight - FOOTER_H + (FOOTER_H - BTN_H) / 2;
            int nextButtonX = dialogX + PAD;
            for (ButtonComponent button : buttonWidgets) {
                button.mount(nextButtonX, buttonY, buttonWidth, BTN_H);
                nextButtonX += buttonWidth + BTN_GAP;
            }
        }
    }

    private int totalDialogHeight() {
        int inner = (content != null) ? PAD * 2 + dialogHeight : PAD;
        return HEADER_H + inner + FOOTER_H;
    }

    @Override
    public @NonNull List<BaseComponent<?>> children() {
        List<BaseComponent<?>> list = new ArrayList<>();
        if (content != null) list.add(content);
        list.addAll(buttonWidgets);
        return list;
    }

    // ── Input — modal: absorb all events while visible ────────

    @Override
    public boolean blocksLowerInput() {
        return open;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // Consume the whole viewport while the dialog is open
        return open
                && mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!open) return false;

        // Buttons first
        for (ButtonComponent button : buttonWidgets) {
            if (button.mouseClicked(event, doubleClick)) return true;
        }

        // Content
        if (content != null && content.mouseClicked(event, doubleClick)) return true;

        // Backdrop click — close if enabled and click was outside the box
        if (closeOnBackdrop && !isOverBox(event.x(), event.y())) {
            close();
        }

        return true; // always absorb when open
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        for (ButtonComponent button : buttonWidgets) button.mouseReleased(event);
        return open;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (!open) return false;
        if (content != null) content.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (!open) return false;
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        return getFocused() != null && getFocused().keyPressed(event);
    }

    private boolean isOverBox(double mouseX, double mouseY) {
        int dialogBoxWidth = dialogWidth();
        int dialogBoxHeight = totalDialogHeight();
        int dialogX = x + (width - dialogBoxWidth) / 2;
        int dialogY = y + (height - dialogBoxHeight) / 2;
        return mouseX >= dialogX && mouseX < dialogX + dialogBoxWidth && mouseY >= dialogY && mouseY < dialogY + dialogBoxHeight;
    }

    // ── Rendering ─────────────────────────────────────────────

    @Override
    public void extract(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        openProgress = stepAnimation(openProgress, open, 0.4f, delta);
        if (openProgress < 0.01f) return;

        UITheme theme = UITheme.current();

        int dialogBoxWidth = dialogWidth();
        int dialogBoxHeight = totalDialogHeight();
        int cx = x + width  / 2;
        int cy = y + height / 2;
        int dialogX = cx - dialogBoxWidth / 2;
        int dialogY = cy - dialogBoxHeight / 2;

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
        g.fill(dialogX + 2, dialogY + 2, dialogX + dialogBoxWidth - 2, dialogY + dialogBoxHeight - 2, theme.panel().bg());

        // Outer border
        g.outline(dialogX,     dialogY,     dialogBoxWidth,     dialogBoxHeight,     theme.panel().border());
        g.outline(dialogX + 1, dialogY + 1, dialogBoxWidth - 2, dialogBoxHeight - 2, theme.panel().border());

        // Header background
        g.fill(dialogX + 2, dialogY + 2, dialogX + dialogBoxWidth - 2, dialogY + HEADER_H, theme.panel().headerBg());

        // Title — centred in header
        g.centeredText(font, title, cx, dialogY + (HEADER_H - font.lineHeight) / 2,
                ARGB.srgbLerp(openProgress, theme.text().secondary(), theme.text().primary()));

        // Divider under header
        g.fill(dialogX + 2, dialogY + HEADER_H, dialogX + dialogBoxWidth - 2, dialogY + HEADER_H + 1, theme.panel().border());

        // Content
        if (content != null) content.extractRenderState(g, mouseX, mouseY, delta);

        // Divider above footer
        int footerDividerY = dialogY + dialogBoxHeight - FOOTER_H;
        g.fill(dialogX + 2, footerDividerY, dialogX + dialogBoxWidth - 2, footerDividerY + 1, theme.panel().border());

        // Buttons
        for (ButtonComponent button : buttonWidgets) {
            button.extractRenderState(g, mouseX, mouseY, delta);
        }

        g.pose().popMatrix();
    }

    // ── Helpers ───────────────────────────────────────────────
    private static float lerp(float t, float from, float to) {
        return from + (to - from) * t;
    }

    private int dialogWidth() {
        return Math.max(0, Math.min(dialogWidth, width - PAD * 4));
    }

    /** Cubic ease-out so the open animation decelerates nicely. */
    private static float easeOut(float t) {
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }
}
