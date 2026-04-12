package com.sypztep.plateau.client.v1.ui.core;

import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

@Environment(EnvType.CLIENT)
public final class RenderHelper {
    private RenderHelper() {}

    public static void rectangle(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + h, color);
    }

    public static void border(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static void panel(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                             int color, int borderColor) {
        rectangle(graphics, x, y, w, h, color);
        border(graphics, x, y, w, h, borderColor);
    }

    public static void panelWithHover(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                                      float hoverProgress, boolean border) {
        UITheme theme = UITheme.current();
        rectangle(graphics, x, y, w, h, ARGB.srgbLerp(hoverProgress, theme.panelBg(), theme.panelBgHover()));
        if (border)
            border(graphics, x, y, w, h, ARGB.srgbLerp(hoverProgress, theme.panelBorder(), theme.panelBorderHover()));
    }

    public static int header(GuiGraphicsExtractor graphics, Font font, Component title,
                             int x, int y, int width, int padding, float hoverProgress) {
        UITheme theme = UITheme.current();
        int headerH = font.lineHeight + padding * 2;
        rectangle(graphics, x + 1, y + 1, width - 2, headerH - 1,
                ARGB.srgbLerp(hoverProgress, theme.panelHeaderBg(), UIColors.lighten(theme.panelHeaderBg(), 0.08f)));
        graphics.centeredText(font, title, x + width / 2, y + padding,
                ARGB.srgbLerp(hoverProgress * 0.3f, theme.textAccent(), theme.textPrimary()));
        return headerH;
    }

    public static void progressBar(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                                   float ratio, int fillColor, int bgColor) {
        rectangle(graphics, x, y, width, height, bgColor);
        int fillW = (int)(width * ratio);
        if (fillW > 0) {
            rectangle(graphics, x, y, fillW, height, fillColor);
            rectangle(graphics, x, y, fillW, Math.max(1, height / 3), UIColors.lighten(fillColor, 0.3f));
        }
    }

    @Deprecated public static void drawRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) { rectangle(g, x, y, w, h, c); }
    @Deprecated public static void drawBorder(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) { border(g, x, y, w, h, c); }
    @Deprecated public static void drawPanel(GuiGraphicsExtractor g, int x, int y, int w, int h, int c, int bc) { panel(g, x, y, w, h, c, bc); }
    @Deprecated public static void drawPanelWithHover(GuiGraphicsExtractor g, int x, int y, int w, int h, float p, boolean b) { panelWithHover(g, x, y, w, h, p, b); }
    @Deprecated public static int drawHeader(GuiGraphicsExtractor g, Font f, Component t, int x, int y, int w, int p, float hp) { return header(g, f, t, x, y, w, p, hp); }
    @Deprecated public static void drawProgressBar(GuiGraphicsExtractor g, int x, int y, int w, int h, float r, int fc, int bc) { progressBar(g, x, y, w, h, r, fc, bc); }
}