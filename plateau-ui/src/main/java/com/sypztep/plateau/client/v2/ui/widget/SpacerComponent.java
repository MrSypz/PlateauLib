package com.sypztep.plateau.client.v2.ui.widget;

import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Insets;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.core.Surface;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Empty space. Use {@code Sizing.fill()} to push siblings apart, or {@code Sizing.fixed(n)} for a fixed gap. */
@Environment(EnvType.CLIENT)
public class SpacerComponent extends BaseComponent {

    public SpacerComponent(int size) {
        this.horizontalSizing = Sizing.fixed(size);
        this.verticalSizing   = Sizing.fixed(size);
    }

    public SpacerComponent(Sizing horizontal, Sizing vertical) {
        this.horizontalSizing = horizontal;
        this.verticalSizing   = vertical;
    }

    @Override
    public void extract(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {}

    @Override public SpacerComponent padding(Insets padding)  { super.padding(padding); return this; }
    @Override public SpacerComponent margins(Insets margins)  { super.margins(margins); return this; }
    @Override public SpacerComponent surface(Surface surface) { super.surface(surface); return this; }
    @Override public SpacerComponent id(String id)            { super.id(id);           return this; }
    @Override public SpacerComponent visible(boolean visible) { super.visible(visible); return this; }
    @Override public SpacerComponent sizing(Sizing h, Sizing v){ super.sizing(h, v);    return this; }
    @Override public SpacerComponent sizing(Sizing both)      { super.sizing(both);     return this; }
}
