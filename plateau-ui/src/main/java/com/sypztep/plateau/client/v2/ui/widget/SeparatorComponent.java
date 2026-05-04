package com.sypztep.plateau.client.v2.ui.widget;

import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Insets;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.core.Surface;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** A 1-pixel divider line, horizontal by default. */
@Environment(EnvType.CLIENT)
public class SeparatorComponent extends BaseComponent {

    public enum Axis { HORIZONTAL, VERTICAL }

    private final Axis axis;
    private int color = -1; // -1 = use theme

    public SeparatorComponent() {
        this(Axis.HORIZONTAL);
    }

    public SeparatorComponent(Axis axis) {
        this.axis = axis;
        if (axis == Axis.HORIZONTAL) {
            this.horizontalSizing = Sizing.fill();
            this.verticalSizing   = Sizing.fixed(1);
        } else {
            this.horizontalSizing = Sizing.fixed(1);
            this.verticalSizing   = Sizing.fill();
        }
    }

    public SeparatorComponent color(int color) { this.color = color; return this; }

    @Override public SeparatorComponent padding(Insets padding)  { super.padding(padding); return this; }
    @Override public SeparatorComponent margins(Insets margins)  { super.margins(margins); return this; }
    @Override public SeparatorComponent surface(Surface surface) { super.surface(surface); return this; }
    @Override public SeparatorComponent id(String id)            { super.id(id);           return this; }
    @Override public SeparatorComponent visible(boolean visible) { super.visible(visible); return this; }
    @Override public SeparatorComponent sizing(Sizing h, Sizing v){ super.sizing(h, v);    return this; }
    @Override public SeparatorComponent sizing(Sizing both)      { super.sizing(both);     return this; }

    @Override
    public int determineHorizontalContentSize(int space) { return axis == Axis.VERTICAL ? 1 : space; }
    @Override
    public int determineVerticalContentSize(int space)   { return axis == Axis.HORIZONTAL ? 1 : space; }

    @Override
    public void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int c = color != -1 ? color : UITheme.current().panelBorder();
        g.fill(x, y, x + width, y + height, c);
    }
}
