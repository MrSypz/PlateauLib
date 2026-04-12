package com.sypztep.plateau.client.v1.ui.widget;

import com.sypztep.plateau.client.v1.ui.core.*;
import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class UIButton extends UIComponent {
    private final Component label;
    @Nullable private final Identifier icon;
    @Nullable private final Consumer<UIButton> onClick;

    private boolean enabled = true;
    private boolean pressed = false;
    private boolean wasHovered = false;

    private float hoverAnimation = 0f;
    private float pressAnimation = 0f;
    private float scaleAnimation = 1.0f;

    private float glowIntensity = 1.0f;
    private float bounceIntensity = 1.0f;
    private float shadowIntensity = 1.0f;
    private boolean roundedCorners = true;
    private int cornerRadius = 4;

    public UIButton(int x, int y, int width, int height, Component label,
                    @Nullable Identifier icon, @Nullable Consumer<UIButton> onClick) {
        super(x, y, width, height);
        this.label = label;
        this.icon = icon;
        this.onClick = onClick;
        this.narrationMessage = label;
        this.soundConfig = SoundConfig.button();
    }

    public UIButton(int x, int y, int width, int height, Component label,
                    @Nullable Consumer<UIButton> onClick) {
        this(x, y, width, height, label, null, onClick);
    }

    @Override
    protected void renderComponent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        boolean hovered = isMouseOver(mouseX, mouseY) && enabled;
        setHoveredCache(hovered);

        hoverAnimation = stepAnimation(hoverAnimation, hovered || focused, 0.05f);
        updatePressAndScale(hovered);

        // Hover sound
        if (hovered && !wasHovered) {
            soundConfig.playHover();
        }
        wasHovered = hovered;

        UITheme theme = UITheme.current();
        int bgColor = calculateBackground(theme);
        int textColor = calculateTextColor(theme);

        graphics.pose().pushMatrix();
        float scale = 1.0f + ((scaleAnimation - 1.0f) * bounceIntensity);
        graphics.pose().translate(x + width / 2f, y + height / 2f);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-(x + width / 2f), -(y + height / 2f));

        // Shadow
        if (shadowIntensity > 0 && !pressed) {
            int offset = (int)(2 * shadowIntensity);
            graphics.fill(x + offset, y + offset,
                    x + width + offset, y + height + offset, 0x66000000);
        }

        // Background
        if (roundedCorners && cornerRadius > 0) {
            graphics.fill(x + cornerRadius, y, x + width - cornerRadius, y + height, bgColor);
            graphics.fill(x, y + cornerRadius, x + width, y + height - cornerRadius, bgColor);
        } else RenderHelper.rectangle(graphics, x, y, width, height, bgColor);


        // Gradient
        int gradientH = (int)(height * 0.15f);
        if (gradientH > 0) {
            RenderHelper.rectangle(graphics, x, y, width, gradientH, UIColors.lighten(bgColor, 0.2f));
            RenderHelper.rectangle(graphics, x, y + height - gradientH, width, gradientH, ARGB.scaleRGB(bgColor, 0.2f));
        }

        // Content
        drawContent(graphics, textColor);

        // Glow
        if (hoverAnimation > 0.3f && enabled && glowIntensity > 0) {
            int glow = ARGB.white((int)(40 * hoverAnimation * glowIntensity));
            RenderHelper.drawBorder(graphics, x - 1, y - 1, width + 2, height + 2, glow);
        }

        // Focus ring
        if (focused && enabled) {
            RenderHelper.drawBorder(graphics, x - 2, y - 2, width + 4, height + 4, 0xFFFFFFFF);
        }

        graphics.pose().popMatrix();

        if (!isMouseOver(mouseX, mouseY)) pressed = false;
    }

    private void updatePressAndScale(boolean hovered) {
        if (pressed) {
            pressAnimation = Math.min(1.0f, pressAnimation + 0.2f);
            scaleAnimation = Math.max(0.95f, 1.0f - (pressAnimation * 0.05f));
        } else {
            pressAnimation = Math.max(0.0f, pressAnimation - 0.1f);
            scaleAnimation = Math.min(1.0f, scaleAnimation + 0.1f);
        }

        if (hovered && enabled) {
            scaleAnimation = Math.min(1.05f, scaleAnimation + 0.01f);
        } else {
            scaleAnimation = Math.max(1.0f, scaleAnimation - 0.01f);
        }
    }

    private void drawContent(GuiGraphicsExtractor graphics, int textColor) {
        int textW = font.width(label);
        int textX;
        int textY = y + (height - font.lineHeight) / 2;
        int iconSize = 16;

        if (icon != null) {
            int totalW = textW + iconSize + 5;
            int startX = x + (width - totalW) / 2;
            int iconY = y + (height - iconSize) / 2;
            iconY += (int)(pressAnimation * 1.5f);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, startX, iconY, iconSize, iconSize);
            textX = startX + iconSize + 5;
        } else {
            textX = x + (width - textW) / 2;
        }

        textY += (int)(pressAnimation * 1.5f);
        graphics.text(font, label, textX, textY, textColor, true);
    }

    private int calculateBackground(UITheme theme) {
        if (!enabled) return theme.buttonBgDisabled();
        return ARGB.srgbLerp(pressAnimation,
                ARGB.srgbLerp(hoverAnimation, theme.buttonBg(), theme.buttonBgHover()),
                theme.buttonBgPressed());
    }

    private int calculateTextColor(UITheme theme) {
        if (!enabled) return theme.textDisabled();
        return ARGB.srgbLerp(hoverAnimation, theme.buttonText(), theme.buttonTextHover());
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (isMouseOver(event.x(), event.y()) && event.button() == GLFW.GLFW_MOUSE_BUTTON_1 && enabled) {
            activate();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) {
        if (!enabled || !focused) return false;
        if (keyEvent.key() == 257 || keyEvent.key() == 32) {
            activate();
            return true;
        }
        return false;
    }

    private void activate() {
        pressed = true;
        soundConfig.playClick();
        if (onClick != null) onClick.accept(this);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (event.button() == 0) pressed = false;
        return false;
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, label);
        if (!enabled) {
            output.add(NarratedElementType.USAGE, Component.translatable("narration.button.disabled"));
        } else if (focused) {
            output.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.focused"));
        } else {
            output.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"));
        }
    }

    // Fluent setters
    public UIButton setEnabled(boolean enabled) { this.enabled = enabled; return this; }
    public UIButton setGlowIntensity(float v) { this.glowIntensity = v; return this; }
    public UIButton setBounceIntensity(float v) { this.bounceIntensity = v; return this; }
    public UIButton setShadowIntensity(float v) { this.shadowIntensity = v; return this; }
    public UIButton setRoundedCorners(boolean rounded, int radius) {
        this.roundedCorners = rounded; this.cornerRadius = radius; return this;
    }
    public UIButton setPlaySounds(boolean hover, boolean click) {
        this.soundConfig.setHoverEnabled(hover).setClickEnabled(click);
        return this;
    }
    public boolean isEnabled() { return enabled; }
}
