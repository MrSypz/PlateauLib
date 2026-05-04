package com.sypztep.plateau.client.v2.ui.widget;

import com.sypztep.plateau.client.v1.ui.core.RenderHelper;
import com.sypztep.plateau.client.v1.ui.core.UISounds;
import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Insets;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.core.Surface;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class ButtonComponent extends BaseComponent {

    private Component label;
    private @Nullable Identifier icon;
    private @Nullable Consumer<ButtonComponent> onClick;
    private boolean enabled = true;
    private float hoverProgress = 0f;
    private boolean pressed = false;

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
    public void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        boolean hovered = enabled && isMouseOver(mouseX, mouseY);
        hoverProgress = stepAnimation(hoverProgress, hovered || (focused && enabled), 0.1f);

        UITheme theme = UITheme.current();
        int bg = pressed && hovered
                ? theme.buttonBgPressed()
                : ARGB.srgbLerp(hoverProgress, theme.buttonBg(), theme.buttonBgHover());

        g.fill(x, y, x + width, y + height, bg);
        RenderHelper.border(g, x, y, width, height, ARGB.srgbLerp(hoverProgress, theme.panelBorder(), theme.panelBorderHover()));

        int textColor = enabled
                ? ARGB.srgbLerp(hoverProgress, theme.buttonText(), theme.buttonTextHover())
                : theme.textDisabled();

        int contentW = font.width(label) + (icon != null ? 16 + 5 : 0);
        int curX = x + (width - contentW) / 2;
        int textY = y + (height - font.lineHeight) / 2;

        if (icon != null) {
            int iconY = y + (height - 16) / 2;
            g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, icon, curX, iconY, 16, 16);
            curX += 16 + 5;
        }
        g.text(font, label, curX, textY, textColor, true);
    }

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

    // Fluent
    public ButtonComponent onClick(Consumer<ButtonComponent> onClick) { this.onClick = onClick; return this; }
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
