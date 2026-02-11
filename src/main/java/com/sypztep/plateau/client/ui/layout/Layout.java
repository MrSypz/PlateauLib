package com.sypztep.plateau.client.ui.layout;

/**
 * Static layout math helpers. Not a layout engine — just removes the
 * hardcoded arithmetic from buildWidgets().
 *
 * Use these to compute positions relative to screen/parent dimensions
 * so things reposition on resize (Tab.buildWidgets is called again on resize).
 */
public final class Layout {
    private Layout() {}

    /** Center a width within a container width, return the x offset. */
    public static int centerX(int containerWidth, int elementWidth) {
        return (containerWidth - elementWidth) / 2;
    }

    /** Center horizontally in screen. */
    public static int centerX(int containerX, int containerWidth, int elementWidth) {
        return containerX + (containerWidth - elementWidth) / 2;
    }

    /** Percentage of a dimension. */
    public static int percent(int total, float pct) {
        return (int)(total * pct);
    }

    /** Clamp a width to min/max, useful for responsive panels. */
    public static int clampWidth(int width, int min, int max) {
        return Math.max(min, Math.min(max, width));
    }

    /** Calculate even spacing for N items in a container. */
    public static int spacing(int containerHeight, int itemCount, int itemHeight) {
        if (itemCount <= 1) return 0;
        int totalItems = itemCount * itemHeight;
        return (containerHeight - totalItems) / (itemCount + 1);
    }
}