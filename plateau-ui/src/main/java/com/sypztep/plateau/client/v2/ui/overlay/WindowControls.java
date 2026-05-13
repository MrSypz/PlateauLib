package com.sypztep.plateau.client.v2.ui.overlay;

import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@Environment(EnvType.CLIENT)
public final class WindowControls {
    public static final int SIZE = 10;
    public static final int X_OFFSET = 7;
    public static final int Y_OFFSET = 5;
    public static final int GAP = 5;

    public enum Type {
        CLOSE(0xFFB65A5A),
        OPEN(0xFF5AB66A),
        MINIMIZE(0xFFE0B85A);

        private final int color;

        Type(int color) {
            this.color = color;
        }

        public int color() {
            return color;
        }
    }

    private WindowControls() {}

    public static int x(int originX, int index) {
        return originX + X_OFFSET + index * (SIZE + GAP);
    }

    public static int y(int originY) {
        return originY + Y_OFFSET;
    }

    public static int titleX(int originX, int controlCount) {
        if (controlCount <= 0) return originX + X_OFFSET;
        return originX + X_OFFSET + controlCount * SIZE + Math.max(0, controlCount - 1) * GAP + 7;
    }

    public static boolean hit(double mouseX, double mouseY, int controlX, int controlY) {
        return mouseX >= controlX && mouseX < controlX + SIZE && mouseY >= controlY && mouseY < controlY + SIZE;
    }

    public static void draw(GuiGraphicsExtractor graphics, Type type, int controlX, int controlY, boolean hovered) {
        UITheme theme = UITheme.current();
        int border = hovered ? theme.panel().borderHover() : theme.panel().border();
        graphics.fill(controlX, controlY, controlX + SIZE, controlY + SIZE, type.color());
        graphics.outline(controlX, controlY, SIZE, SIZE, border);
    }
}
