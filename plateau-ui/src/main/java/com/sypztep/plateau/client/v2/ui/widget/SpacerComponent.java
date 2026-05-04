package com.sypztep.plateau.client.v2.ui.widget;

import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
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
    public int determineHorizontalContentSize(int space) { return 0; }
    @Override
    public int determineVerticalContentSize(int space)   { return 0; }

    @Override
    public void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {}
}
