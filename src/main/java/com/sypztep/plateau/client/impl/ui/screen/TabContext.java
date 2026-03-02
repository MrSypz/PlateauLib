package com.sypztep.plateau.client.impl.ui.screen;

import com.sypztep.plateau.client.impl.ui.layout.Layout;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Pre-computed layout values for tab content. Eliminates the 5-6 boilerplate
 * lines every tab currently starts with.
 */
@Environment(EnvType.CLIENT)
public record TabContext(
        int screenWidth,
        int screenHeight,
        int contentStartY,
        int availableHeight,
        int defaultPanelWidth,
        int defaultPanelX
) {
    public static TabContext from(PlateauScreen screen) {
        int sw = screen.width;
        int sh = screen.height;
        int startY = screen.getContentStartY();
        int panelW = Layout.clampWidth(sw - 40, 200, 500);
        int panelX = Layout.centerX(sw, panelW);
        return new TabContext(sw, sh, startY, sh - startY - 20, panelW, panelX);
    }

    /** Compute a centered panel width with custom constraints. */
    public int panelWidth(int marginTotal, int min, int max) {
        return Layout.clampWidth(screenWidth - marginTotal, min, max);
    }

    /** Center an element of given width on screen. */
    public int centerX(int elementWidth) {
        return Layout.centerX(screenWidth, elementWidth);
    }
}
