package com.sypztep.plateau.client.impl.ui.core;

import com.sypztep.plateau.client.impl.ui.theme.UITheme;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

@Environment(EnvType.CLIENT)
public final class RenderHelper {
    private RenderHelper() {}

    /**
     * Draws a 1px border (top, bottom, left, right).
     * Replaces the 4x graphics.fill() pattern used everywhere.
     */
    public static void drawBorder(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    /**
     * Background fill + border in one call.
     */
    public static void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                                  int bgColor, int borderColor) {
        graphics.fill(x, y, x + w, y + h, bgColor);
        drawBorder(graphics, x, y, w, h, borderColor);
    }

    /**
     * Background + border with hover animation using current theme colors.
     * The most repeated pattern in the entire codebase.
     */
    public static void drawPanelWithHover(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                                           float hoverProgress, boolean drawBorder) {
        UITheme theme = UITheme.current();
        int bg = ARGB.srgbLerp(hoverProgress,theme.panelBg(), theme.panelBgHover());
        graphics.fill(x, y, x + w, y + h, bg);
        if (drawBorder) {
            int border = ARGB.srgbLerp( hoverProgress, theme.panelBorder(), theme.panelBorderHover());
            RenderHelper.drawBorder(graphics, x, y, w, h, border);
        }
    }

    /**
     * Header bar inside a panel. Returns the header height.
     * Eliminates duplicated header rendering in UIPanel and UIScrollPanel.
     */
    public static int drawHeader(GuiGraphicsExtractor graphics, Font font, Component title,
                                  int x, int y, int width, int padding, float hoverProgress) {
        UITheme theme = UITheme.current();
        int headerH = font.lineHeight + padding * 2;
        int headerBg =  ARGB.srgbLerp(hoverProgress,theme.panelHeaderBg(),
                UIColors.lighten(theme.panelHeaderBg(), 0.08f));
        graphics.fill(x + 1, y + 1, x + width - 1, y + headerH, headerBg);

        int titleColor = ARGB.srgbLerp(hoverProgress * 0.3f,theme.textAccent(), theme.textPrimary());
        graphics.centeredText(font, title, x + width / 2, y + padding, titleColor);
        return headerH;
    }

    /**
     * Progress/HP bar with highlight effect.
     */
    public static void drawProgressBar(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
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
