package com.sypztep.plateau.client.v2.ui.core;

import com.sypztep.plateau.client.v1.ui.core.RenderHelper;
import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@FunctionalInterface
public interface Surface {

    void draw(GuiGraphicsExtractor graphics, int x, int y, int width, int height);

    Surface NONE = (g, x, y, w, h) -> {};

    static Surface flat(int color) {
        return (g, x, y, w, h) -> g.fill(x, y, x + w, y + h, color);
    }

    static Surface panel() {
        return (g, x, y, w, h) -> RenderHelper.panel(g, x, y, w, h,
                UITheme.current().panelBg(), UITheme.current().panelBorder());
    }

    static Surface outline(int color) {
        return (g, x, y, w, h) -> RenderHelper.border(g, x, y, w, h, color);
    }

    static Surface outline() {
        return (g, x, y, w, h) -> RenderHelper.border(g, x, y, w, h, UITheme.current().panelBorder());
    }

    static Surface gradient(int topColor, int bottomColor) {
        return (g, x, y, w, h) -> g.fillGradient(x, y, x + w, y + h, topColor, bottomColor);
    }

    default Surface andThen(Surface next) {
        return (g, x, y, w, h) -> { this.draw(g, x, y, w, h); next.draw(g, x, y, w, h); };
    }
}
