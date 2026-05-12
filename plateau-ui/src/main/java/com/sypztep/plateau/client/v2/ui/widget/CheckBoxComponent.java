package com.sypztep.plateau.client.v2.ui.widget;

import com.sypztep.plateau.client.v1.ui.core.UISounds;
import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Insets;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.core.Surface;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class CheckBoxComponent extends BaseComponent {
    private static final Identifier CHECKBOX_SPRITE = Identifier.withDefaultNamespace("pending_invite/accept");

    private static final int BOX = 13;
    private static final int GAP = 5;

    private Component label;
    private boolean checked;
    private boolean enabled = true;
    private Consumer<Boolean> onChanged = ignored -> {};
    private float hoverProgress = 0f;
    private float checkProgress;
    private boolean wasHovered = false;

    public CheckBoxComponent(Component label, boolean checked) {
        this.label = label;
        this.checked = checked;
        this.checkProgress = checked ? 1f : 0f;
        this.horizontalSizing = Sizing.content();
        this.verticalSizing = Sizing.fixed(18);
    }

    @Override
    public int determineHorizontalContentSize(int space) {
        return Math.min(Math.max(0, space), BOX + GAP + font.width(label) + padding.horizontal());
    }

    @Override
    public int determineVerticalContentSize(int space) { return 18 + padding.vertical(); }

    @Override
    protected boolean isFocusable() { return enabled; }

    @Override
    public void extract(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (width <= 0 || height <= 0) return;

        UITheme theme = UITheme.current();
        boolean hovered = enabled && isMouseOver(mouseX, mouseY);
        if (hovered && !wasHovered) UISounds.playHover();
        wasHovered = hovered;
        boolean hot = enabled && (focused || hovered);
        hoverProgress = stepAnimation(hoverProgress, hot, 0.5f, delta);
        checkProgress = stepAnimation(checkProgress, checked, 0.55f, delta);
        int boxX = innerX();
        int boxY = innerY() + (innerHeight() - BOX) / 2;
        int border = ARGB.srgbLerp(hoverProgress, theme.panel().border(), theme.panel().borderHover());
        int bg = ARGB.srgbLerp(hoverProgress, theme.panel().bg(), theme.panel().bgHover());
        int text = enabled ? theme.text().primary() : theme.text().disabled();
        g.fill(boxX, boxY, boxX + BOX, boxY + BOX, enabled ? bg : theme.button().bg().disabled());
        g.outline(boxX, boxY, BOX, BOX, border);

        if (checkProgress > 0.01f) {
            int c = enabled ? theme.text().accent() : theme.text().disabled();
            int color = ARGB.color((int)(checkProgress * 255f), c);
            int iconSize = 16;
            int iconX = boxX + (BOX - iconSize) / 2;
            int iconY = boxY + (BOX - iconSize) / 2;
            g.blitSprite(RenderPipelines.GUI_TEXTURED, CHECKBOX_SPRITE, iconX, iconY, iconSize, iconSize, color);
        }

        int textX = boxX + BOX + GAP;
        g.enableScissor(textX, y, innerX() + innerWidth(), y + height);
        g.text(font, label, textX, innerY() + (innerHeight() - font.lineHeight) / 2, text, true);
        g.disableScissor();
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!enabled || event.button() != 0 || !isMouseOver(event.x(), event.y())) return false;
        toggle();
        return true;
    }

    @Override
    public boolean onPointerClicked(MouseButtonEvent event, boolean doubleClick, double x, double y) {
        if (!enabled || event.button() != 0 || !hitTest(x, y)) return false;
        toggle();
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (!enabled || !focused) return false;
        int key = event.key();
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            toggle();
            return true;
        }
        return false;
    }

    public CheckBoxComponent toggle() { return checked(!checked); }

    public CheckBoxComponent checked(boolean checked) {
        if (this.checked != checked) {
            this.checked = checked;
            UISounds.playClick();
            onChanged.accept(this.checked);
        }
        return this;
    }

    public boolean checked() { return checked; }
    public CheckBoxComponent onChanged(Consumer<Boolean> onChanged) { this.onChanged = onChanged != null ? onChanged : ignored -> {}; return this; }
    public CheckBoxComponent label(Component label) { this.label = label; return this; }
    public CheckBoxComponent enabled(boolean enabled) { this.enabled = enabled; return this; }

    @Override public CheckBoxComponent padding(Insets padding) { super.padding(padding); return this; }
    @Override public CheckBoxComponent margins(Insets margins) { super.margins(margins); return this; }
    @Override public CheckBoxComponent surface(Surface surface) { super.surface(surface); return this; }
    @Override public CheckBoxComponent id(String id) { super.id(id); return this; }
    @Override public CheckBoxComponent visible(boolean visible) { super.visible(visible); return this; }
    @Override public CheckBoxComponent sizing(Sizing h, Sizing v) { super.sizing(h, v); return this; }
    @Override public CheckBoxComponent sizing(Sizing both) { super.sizing(both); return this; }
}
