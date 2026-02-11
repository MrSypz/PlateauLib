package com.sypztep.plateau.client.ui.widget;

import com.sypztep.plateau.client.ui.core.UIComponent;
import com.sypztep.plateau.client.ui.theme.UITheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class UILabel extends UIComponent {
    private Component text;
    private int color = UITheme.TEXT_PRIMARY;
    private boolean shadow = true;
    private boolean centered = false;

    public UILabel(int x, int y, Component text) {
        super(x, y, 0, 0);
        this.text = text;
        this.width = font.width(text);
        this.height = font.lineHeight;
        this.focusable = false; // labels don't take focus
    }

    public UILabel(int x, int y, int width, Component text) {
        super(x, y, width, font0().lineHeight);
        this.text = text;
        this.centered = true;
        this.focusable = false;
    }

    private static net.minecraft.client.gui.Font font0() {
        return net.minecraft.client.Minecraft.getInstance().font;
    }

    @Override
    protected void renderComponent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (centered) {
            graphics.drawCenteredString(font, text, x + width / 2, y, color);
        } else {
            graphics.drawString(font, text, x, y, color, shadow);
        }
    }

    public UILabel setText(Component text) {
        this.text = text;
        if (!centered) this.width = font.width(text);
        return this;
    }
    public UILabel setColor(int color) { this.color = color; return this; }
    public UILabel setShadow(boolean shadow) { this.shadow = shadow; return this; }
    public UILabel setCentered(boolean centered) { this.centered = centered; return this; }
}