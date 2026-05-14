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
    private static final int DEFAULT_PREVIEW_BG = 0xEE15171E;
    private static final int DEFAULT_PREVIEW_TEXT = 0xFFFFFFFF;

    private RenderHelper() {}

    public static void rectangle(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + h, color);
    }

    @Deprecated
    public static void outline(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        graphics.outline(x, y, width, height, color);
    }

    public static void panel(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                             int color, int borderColor) {
        rectangle(graphics, x, y, w, h, color);
        outline(graphics, x, y, w, h, borderColor);
    }

    public static void floatingPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                                     int bgColor, int borderColor) {
        floatingPanel(graphics, x, y, width, height, 0x66000000, bgColor, borderColor);
    }

    public static void floatingPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                                     int shadowColor, int bgColor, int borderColor) {
        graphics.fill(x + 1, y + 1, x + width + 1, y + height + 1, shadowColor);
        graphics.fill(x, y, x + width, y + height, bgColor);
        graphics.outline(x, y, width, height, borderColor);
    }

    public static void swatchLabelPreview(GuiGraphicsExtractor graphics, Font font, Component label,
                                          int x, int y, int minWidth, int height,
                                          int swatchColor) {
        swatchLabelPreview(graphics, font, label,
                x, y, minWidth, height,
                swatchColor, DEFAULT_PREVIEW_BG, swatchColor, DEFAULT_PREVIEW_TEXT, true);
    }

    public static void swatchLabelPreview(GuiGraphicsExtractor graphics, Font font, Component label,
                                          int x, int y, int minWidth, int height,
                                          int swatchColor, int bgColor, int borderColor, int textColor,
                                          boolean shadow) {
        int swatchSize = Math.max(0, height - 10);
        int previewWidth = Math.max(minWidth, font.width(label) + swatchSize + 18);

        floatingPanel(graphics, x, y, previewWidth, height, bgColor, borderColor);

        if (swatchSize > 0) {
            int swatchX = x + 5;
            int swatchY = y + (height - swatchSize) / 2;
            graphics.fill(swatchX, swatchY, swatchX + swatchSize, swatchY + swatchSize, swatchColor);
        }

        graphics.text(font, label, x + swatchSize + 10, y + (height - font.lineHeight) / 2, textColor, shadow);
    }

    public static void panelWithHover(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                                      float hoverProgress, boolean border) {
        UITheme theme = UITheme.current();
        UITheme.Panel panel = theme.panel();

        rectangle(graphics, x, y, w, h,
                ARGB.srgbLerp(hoverProgress, panel.bg(), panel.bgHover()));

        if (border) {
            outline(graphics, x, y, w, h,
                    ARGB.srgbLerp(hoverProgress, panel.border(), panel.borderHover()));
        }
    }

    public static void header(GuiGraphicsExtractor graphics, Font font, Component title,
                              int x, int y, int width, int padding, float hoverProgress) {
        UITheme theme = UITheme.current();
        UITheme.Panel panel = theme.panel();
        UITheme.Text text = theme.text();

        int headerH = font.lineHeight + padding * 2;

        rectangle(graphics, x + 1, y + 1, width - 2, headerH - 1,
                ARGB.srgbLerp(hoverProgress, panel.headerBg(), UIColors.lighten(panel.headerBg(), 0.08f)));

        graphics.centeredText(font, title, x + width / 2, y + padding,
                ARGB.srgbLerp(hoverProgress * 0.3f, text.accent(), text.primary()));

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

    public record ButtonColors(int bg, int bgTop, int border, int outline, int underline, int text) {}
    public static ButtonColors buttonColors(boolean enabled, float hoverProgress, float pressProgress) {
        UITheme        theme  = UITheme.current();
        UITheme.Button button = theme.button();

        if (!enabled) {
            return new ButtonColors(
                    button.bg().disabled(),
                    button.bg().disabled(),
                    button.border().disabled(),
                    button.outline().disabled(),
                    button.underline().disabled(),
                    theme.text().disabled()
            );
        }

        int bg = ARGB.srgbLerp(
                pressProgress,
                ARGB.srgbLerp(hoverProgress, button.bg().normal(), button.bg().hover()),
                button.bg().pressed()
        );

        int bgTop = ARGB.srgbLerp(
                pressProgress,
                ARGB.srgbLerp(hoverProgress,
                        bg,                                             // ← idle: bgTop == bg → flat fill
                        ARGB.scaleRGB(button.bg().hover(), 1.5f)),        // ← hover: gradient fades in
                button.bg().pressed()                                   // ← press: flattens again
        );

        int border = ARGB.srgbLerp(
                pressProgress,
                ARGB.srgbLerp(hoverProgress, button.border().normal(), button.border().hover()),
                button.border().pressed()
        );

        int outline = ARGB.srgbLerp(
                pressProgress,
                ARGB.srgbLerp(hoverProgress, button.outline().normal(), button.outline().hover()),
                button.outline().pressed()
        );

        int underline = ARGB.srgbLerp(
                pressProgress,
                ARGB.srgbLerp(hoverProgress, button.underline().normal(), button.underline().hover()),
                button.underline().pressed()
        );

        int text = ARGB.srgbLerp(hoverProgress, button.text().normal(), button.text().hover());

        return new ButtonColors(bg, bgTop, border, outline, underline, text);
    }

    public static void squareButton(GuiGraphicsExtractor graphics, Font font, Component label,
                                    int x, int y, int width, int height,
                                    boolean enabled,
                                    float hoverProgress, float pressProgress,
                                    boolean shadow) {
        ButtonColors colors = buttonColors(enabled, hoverProgress, pressProgress);

        graphics.fillGradient(x + 2, y + 2, x + width - 2, y + height - 4, colors.bg(), colors.bgTop());
        outline(graphics, x, y, width, height, colors.border());
        outline(graphics, x + 1, y + 1, width - 2, height - 4, colors.outline());
        graphics.fill(x + 1, y + height - 3, x + width - 1, y + height - 1, colors.underline());

        int textX = x + (width - font.width(label)) / 2;
        int textY = y + (height - font.lineHeight) / 2;

        graphics.text(font, label, textX, textY, colors.text(), shadow);
    }
}
