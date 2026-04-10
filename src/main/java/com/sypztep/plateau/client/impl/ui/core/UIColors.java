package com.sypztep.plateau.client.impl.ui.core;

import net.minecraft.util.ARGB;

public final class UIColors {
    private UIColors() {}

    public static int lighten(int color, float factor) {
        int a = ARGB.alpha(color);
        int r = Math.min(255, (int)(ARGB.red(color) + (255 - ARGB.red(color)) * factor));
        int g = Math.min(255, (int)(ARGB.green(color) + (255 - ARGB.green(color)) * factor));
        int b = Math.min(255, (int)(ARGB.blue(color) + (255 - ARGB.blue(color)) * factor));
        return ARGB.color(a, r, g, b);
    }
}