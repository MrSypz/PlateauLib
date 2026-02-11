package com.sypztep.plateau.client.ui.widget;

import com.sypztep.plateau.client.ui.core.UIColors;
import com.sypztep.plateau.client.ui.core.UIComponent;
import com.sypztep.plateau.client.ui.theme.UITheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Visual container panel. Draws background, border, optional header.
 * Does NOT intercept input — add as renderable via addRenderable(),
 * not addWidget().
 */
public class UIPanel extends UIComponent {
    @Nullable protected Component title;
    protected boolean drawHeader;
    protected boolean drawBorder = true;
    protected float hoverAnimation = 0f;
    private boolean interactable = false;

    public UIPanel(int x, int y, int width, int height, @Nullable Component title) {
        super(x, y, width, height);
        this.title = title;
        this.drawHeader = title != null;
        this.padding = 10;
        this.focusable = false;
    }

    public UIPanel(int x, int y, int width, int height) {
        this(x, y, width, height, null);
    }

    /**
     * By default panels don't report mouse-over, so Screen's getChildAt
     * skips them and finds the interactive widget underneath.
     * Set interactable(true) if you need the panel itself to handle input.
     */
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!interactable) return false;
        return super.isMouseOver(mouseX, mouseY);
    }

    @Override
    protected void renderComponent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Hover animation only if interactable (otherwise always 0)
        if (interactable) {
            hoverAnimation = stepAnimation(hoverAnimation, super.isMouseOver(mouseX, mouseY), 0.05f);
        }

        // Background
        int bg = UIColors.interpolate(UITheme.PANEL_BG, UITheme.PANEL_BG_HOVER, hoverAnimation);
        graphics.fill(x, y, x + width, y + height, bg);

        // Border
        if (drawBorder) {
            int border = UIColors.interpolate(UITheme.PANEL_BORDER, UITheme.PANEL_BORDER_HOVER, hoverAnimation);
            graphics.fill(x, y, x + width, y + 1, border);
            graphics.fill(x, y + height - 1, x + width, y + height, border);
            graphics.fill(x, y, x + 1, y + height, border);
            graphics.fill(x + width - 1, y, x + width, y + height, border);
        }

        // Header
        if (drawHeader && title != null) {
            int headerH = font.lineHeight + padding * 2;
            int headerBg = UIColors.interpolate(UITheme.PANEL_HEADER_BG, 0xFF2A2A2A, hoverAnimation);
            graphics.fill(x + 1, y + 1, x + width - 1, y + headerH, headerBg);

            int titleColor = UIColors.interpolate(UITheme.TEXT_ACCENT, UITheme.TEXT_PRIMARY, hoverAnimation * 0.3f);
            graphics.drawCenteredString(font, title, x + width / 2, y + padding, titleColor);
        }

        renderContents(graphics, mouseX, mouseY, delta);
    }

    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {}

    @Override
    public int getContentY() {
        return y + (drawHeader && title != null ? font.lineHeight + padding * 3 : padding);
    }

    public UIPanel setTitle(@Nullable Component title) { this.title = title; this.drawHeader = title != null; return this; }
    public UIPanel setDrawHeader(boolean draw) { this.drawHeader = draw; return this; }
    public UIPanel setDrawBorder(boolean draw) { this.drawBorder = draw; return this; }
    public UIPanel setInteractable(boolean interactable) { this.interactable = interactable; return this; }
}