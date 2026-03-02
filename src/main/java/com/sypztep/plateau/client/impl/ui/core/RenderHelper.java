package com.sypztep.plateau.client.impl.ui.core;

import com.sypztep.plateau.client.impl.ui.theme.UITheme;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public final class RenderHelper {
    private RenderHelper() {}

    /**
     * Draws a 1px border (top, bottom, left, right).
     * Replaces the 4x graphics.fill() pattern used everywhere.
     */
    public static void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    /**
     * Background fill + border in one call.
     */
    public static void drawPanel(GuiGraphics graphics, int x, int y, int w, int h,
                                  int bgColor, int borderColor) {
        graphics.fill(x, y, x + w, y + h, bgColor);
        drawBorder(graphics, x, y, w, h, borderColor);
    }

    /**
     * Background + border with hover animation using current theme colors.
     * The most repeated pattern in the entire codebase.
     */
    public static void drawPanelWithHover(GuiGraphics graphics, int x, int y, int w, int h,
                                           float hoverProgress, boolean drawBorder) {
        UITheme theme = UITheme.current();
        int bg = UIColors.interpolate(theme.panelBg(), theme.panelBgHover(), hoverProgress);
        graphics.fill(x, y, x + w, y + h, bg);
        if (drawBorder) {
            int border = UIColors.interpolate(theme.panelBorder(), theme.panelBorderHover(), hoverProgress);
            RenderHelper.drawBorder(graphics, x, y, w, h, border);
        }
    }

    /**
     * Header bar inside a panel. Returns the header height.
     * Eliminates duplicated header rendering in UIPanel and UIScrollPanel.
     */
    public static int drawHeader(GuiGraphics graphics, Font font, Component title,
                                  int x, int y, int width, int padding, float hoverProgress) {
        UITheme theme = UITheme.current();
        int headerH = font.lineHeight + padding * 2;
        int headerBg = UIColors.interpolate(theme.panelHeaderBg(),
                UIColors.lighten(theme.panelHeaderBg(), 0.08f), hoverProgress);
        graphics.fill(x + 1, y + 1, x + width - 1, y + headerH, headerBg);

        int titleColor = UIColors.interpolate(theme.textAccent(), theme.textPrimary(), hoverProgress * 0.3f);
        graphics.drawCenteredString(font, title, x + width / 2, y + padding, titleColor);
        return headerH;
    }

    /**
     * Progress/HP bar with highlight effect.
     */
    public static void drawProgressBar(GuiGraphics graphics, int x, int y, int width, int height,
                                        float ratio, int fillColor, int bgColor) {
        graphics.fill(x, y, x + width, y + height, bgColor);
        int fillW = (int)(width * ratio);
        if (fillW > 0) {
            graphics.fill(x, y, x + fillW, y + height, fillColor);
            int highlightH = Math.max(1, height / 3);
            graphics.fill(x, y, x + fillW, y + highlightH, UIColors.lighten(fillColor, 0.3f));
        }
    }
}
