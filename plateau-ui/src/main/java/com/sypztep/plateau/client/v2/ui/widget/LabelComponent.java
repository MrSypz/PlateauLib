package com.sypztep.plateau.client.v2.ui.widget;

import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Insets;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.core.Surface;
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

    public LabelComponent(Component text) {
        this.text  = text;
        this.color = UITheme.current().text().primary();
        this.horizontalSizing = Sizing.content();
        this.verticalSizing   = Sizing.content();
    }

    @Override
    public int determineHorizontalContentSize(int space) { return Math.min(font.width(text) + padding.horizontal(), Math.max(0, space)); }
    @Override
    public int determineVerticalContentSize(int space)   { return font.lineHeight + padding.vertical(); }

    @Override
    public void extract(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (width <= 0 || height <= 0) return;

        g.enableScissor(x, y, x + width, y + height);
        if (centered) {
            int textX = innerX() + (innerWidth() - font.width(text)) / 2;
            int textY = innerY() + (innerHeight() - font.lineHeight) / 2;
            g.text(font, text, textX, textY, color, shadow);
        } else {
            g.text(font, text, innerX(), innerY() + (innerHeight() - font.lineHeight) / 2, color, shadow);
        }
        g.disableScissor();
    }

    // Fluent
    public LabelComponent text(Component text)     { this.text = text; return this; }
    public LabelComponent color(int color)         { this.color = color; return this; }
    public LabelComponent shadow(boolean shadow)   { this.shadow = shadow; return this; }
    public LabelComponent centered(boolean v)      { this.centered = v; return this; }

    public LabelComponent secondary()  { return color(UITheme.current().text().secondary()); }
    public LabelComponent accent()     { return color(UITheme.current().text().accent()); }
    public LabelComponent disabled()   { return color(UITheme.current().text().disabled()); }

    @Override public LabelComponent padding(Insets padding)  { super.padding(padding); return this; }
    @Override public LabelComponent margins(Insets margins)  { super.margins(margins); return this; }
    @Override public LabelComponent surface(Surface surface) { super.surface(surface); return this; }
    @Override public LabelComponent id(String id)            { super.id(id);           return this; }
    @Override public LabelComponent visible(boolean visible) { super.visible(visible); return this; }
    @Override public LabelComponent sizing(Sizing h, Sizing v){ super.sizing(h, v);    return this; }
    @Override public LabelComponent sizing(Sizing both)      { super.sizing(both);     return this; }
}
