package com.sypztep.plateau.client.impl.ui.core;

/**
 * Centralized color utilities. No more copy-pasting lighten/darken across every widget.
 */
public final class UIColors {

    private UIColors() {}

    public static int interpolate(int color1, int color2, float progress) {
        if (progress <= 0f) return color1;
        if (progress >= 1f) return color2;

        int a1 = (color1 >> 24) & 0xFF, r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF,  b1 = color1 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF, r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF,  b2 = color2 & 0xFF;

        return ((int)(a1 + (a2 - a1) * progress) << 24)
             | ((int)(r1 + (r2 - r1) * progress) << 16)
             | ((int)(g1 + (g2 - g1) * progress) << 8)
             |  (int)(b1 + (b2 - b1) * progress);
    }

    public static int lighten(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, (int)(((color >> 16) & 0xFF) + (255 - ((color >> 16) & 0xFF)) * factor));
        int g = Math.min(255, (int)(((color >> 8) & 0xFF) + (255 - ((color >> 8) & 0xFF)) * factor));
        int b = Math.min(255, (int)((color & 0xFF) + (255 - (color & 0xFF)) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int darken(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = Math.max(0, (int)(((color >> 16) & 0xFF) * (1 - factor)));
        int g = Math.max(0, (int)(((color >> 8) & 0xFF) * (1 - factor)));
        int b = Math.max(0, (int)((color & 0xFF) * (1 - factor)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static int withAlpha(int color, float alpha) {
        return withAlpha(color, (int)(alpha * 255));
    }
}