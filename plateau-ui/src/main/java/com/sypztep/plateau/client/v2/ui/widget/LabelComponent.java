package com.sypztep.plateau.client.v2.ui.widget;

import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class LabelComponent extends BaseComponent {

    private Component text;
    private int color;
    private boolean shadow      = true;
    private boolean centered    = false;
    private boolean smallCaps   = false;

    public LabelComponent(Component text) {
        this.text  = text;
        this.color = UITheme.current().textPrimary();
        this.horizontalSizing = Sizing.content();
        this.verticalSizing   = Sizing.content();
    }

    @Override
    public int determineHorizontalContentSize(int space) { return font.width(text); }
    @Override
    public int determineVerticalContentSize(int space)   { return font.lineHeight; }

    @Override
    public void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (centered) {
            int textX = x + (width - font.width(text)) / 2;
            int textY = y + (height - font.lineHeight) / 2;
            g.text(font, text, textX, textY, color, shadow);
        } else {
            g.text(font, text, x, y + (height - font.lineHeight) / 2, color, shadow);
        }
    }

    // Fluent
    public LabelComponent text(Component text)     { this.text = text; return this; }
    public LabelComponent color(int color)         { this.color = color; return this; }
    public LabelComponent shadow(boolean shadow)   { this.shadow = shadow; return this; }
    public LabelComponent centered(boolean v)      { this.centered = v; return this; }

    public LabelComponent secondary()  { return color(UITheme.current().textSecondary()); }
    public LabelComponent accent()     { return color(UITheme.current().textAccent()); }
    public LabelComponent disabled()   { return color(UITheme.current().textDisabled()); }
}
