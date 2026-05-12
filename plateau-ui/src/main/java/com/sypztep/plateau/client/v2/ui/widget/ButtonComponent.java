package com.sypztep.plateau.client.v2.ui.widget;

import com.sypztep.plateau.client.v1.ui.core.RenderHelper;
import com.sypztep.plateau.client.v1.ui.core.UISounds;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Insets;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.core.Surface;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class ButtonComponent extends BaseComponent {

    private Component label;
    private @Nullable Identifier icon;
    private @Nullable Consumer<ButtonComponent> onClick;
    private boolean enabled = true;
    private float hoverProgress = 0f;
    private float pressProgress = 0f;
    private boolean pressed = false;
    private boolean wasHovered = false;
    private float liftProgress = 0f;

    public ButtonComponent(Component label) {
        this.label = label;
        this.horizontalSizing = Sizing.content();
        this.verticalSizing   = Sizing.fixed(20);
    }

    @Override
    public int determineHorizontalContentSize(int space) {
        int w = font.width(label) + 12;
        if (icon != null) w += 16 + 5;
        return w;
    }

    @Override
    public int determineVerticalContentSize(int space) { return 20; }

    @Override
    protected boolean isFocusable() { return enabled; }

    @Override
    public void extract(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (width <= 0 || height <= 0) return;

        boolean hovered = enabled && isMouseOver(mouseX, mouseY);
        boolean active = hovered || (focused && enabled);
        if (hovered && !wasHovered) UISounds.playHover();
        wasHovered = hovered;

        hoverProgress = stepAnimation(hoverProgress, active, 0.5f, delta);
        liftProgress = stepAnimation(liftProgress, active && !pressed, 0.5f, delta);
        // Self-heal: if the LMB is no longer physically held, clear pressed regardless of
        // whether mouseReleased propagated through the ScrollContainer/FlowLayout chain.
        if (pressed && GLFW.glfwGetMouseButton(
                minecraft.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_1) != GLFW.GLFW_PRESS)
            pressed = false;

        // Snap to full dim on press; ease back to hover on release.
        if (pressed && hovered) pressProgress = 1.0f;
        else pressProgress = Math.max(0f, pressProgress - 0.15f);
        float hoverY = lerp(liftProgress, 0f, -2f);
        float pressY = lerp(pressProgress, 0f, 1.5f);
        float liftY = hoverY + pressY;

        g.pose().pushMatrix();
        g.pose().translate(0f, liftY);
        g.enableScissor(x, y, x + width, y + height);
        if (icon == null) {
            RenderHelper.squareButton(g, font, label, x, y, width, height, enabled, hoverProgress, pressProgress, true);
        } else {
            extractIconButton(g);
        }
        g.disableScissor();

        g.pose().popMatrix();
    }

    private void extractIconButton(GuiGraphicsExtractor extractor) {
        RenderHelper.ButtonColors colors = RenderHelper.buttonColors(enabled, hoverProgress, pressProgress);

        extractor.fillGradient(x + 2, y + 2, x + width - 2, y + height - 4, colors.bg(), colors.bgTop());
        extractor.outline(x, y, width, height, colors.border());
        extractor.outline(x + 1, y + 1, width - 2, height - 4, colors.outline());
        extractor.fill(x + 1, y + height - 3, x + width - 1, y + height - 1, colors.underline());

        int contentW = font.width(label) + 16 + 5;
        int curX = x + (width - contentW) / 2;
        int textY = y + (height - font.lineHeight) / 2;

        int iconY = y + (height - 16) / 2;
        extractor.blitSprite(RenderPipelines.GUI_TEXTURED, icon, curX, iconY, 16, 16);
        curX += 16 + 5;

        extractor.text(font, label, curX, textY, colors.text(), true);
    }
    // Used by the non-scroll path (screen dispatches directly via GuiEventListener chain).
    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!enabled || event.button() != 0) return false;
        if (!isMouseOver(event.x(), event.y())) return false;
        pressed = true;
        UISounds.playClick();
        if (onClick != null) onClick.accept(this);
        return true;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        pressed = false;
        return false;
    }

    // Used by the scroll path (ScrollContainer forwards content-space coords via PointerInteractable).
    @Override
    public boolean onPointerClicked(MouseButtonEvent event, boolean doubleClick, double x, double y) {
        if (!enabled || event.button() != 0) return false;
        if (!hitTest(x, y)) return false;
        pressed = true;
        UISounds.playClick();
        if (onClick != null) onClick.accept(this);
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (!enabled || !focused) return false;

        int key = event.key();
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            pressed = true;
            pressProgress = 1.0f;
            UISounds.playClick();

            if (onClick != null) onClick.accept(this);
            return true;
        }

        return false;
    }

    @Override
    public @NonNull NarrationPriority narrationPriority() {
        if (focused) return NarrationPriority.FOCUSED;
        if (isMouseOver(minecraft.mouseHandler.xpos(), minecraft.mouseHandler.ypos())) return NarrationPriority.HOVERED;
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, label);
    }

    @Override
    public void setFocused(boolean focused) {
        if (focused && !this.focused) UISounds.playFocus();
        super.setFocused(focused);
    }
    private static float lerp(float t, float from, float to) {
        return from + (to - from) * t;
    }
    // Fluent
    public ButtonComponent onClick(Consumer<ButtonComponent> onClick)  { this.onClick = onClick; return this; }
    public ButtonComponent icon(Identifier icon)                       { this.icon = icon; return this; }
    public ButtonComponent enabled(boolean enabled)                    { this.enabled = enabled; return this; }
    public ButtonComponent label(Component label)                      { this.label = label; return this; }
    public boolean isEnabled()                                         { return enabled; }

    @Override public ButtonComponent padding(Insets padding)  { super.padding(padding); return this; }
    @Override public ButtonComponent margins(Insets margins)  { super.margins(margins); return this; }
    @Override public ButtonComponent surface(Surface surface) { super.surface(surface); return this; }
    @Override public ButtonComponent id(String id)            { super.id(id);           return this; }
    @Override public ButtonComponent visible(boolean visible) { super.visible(visible); return this; }
    @Override public ButtonComponent sizing(Sizing h, Sizing v){ super.sizing(h, v);    return this; }
    @Override public ButtonComponent sizing(Sizing both)      { super.sizing(both);     return this; }
}
