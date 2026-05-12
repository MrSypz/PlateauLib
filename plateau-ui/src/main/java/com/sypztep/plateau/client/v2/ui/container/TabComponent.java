package com.sypztep.plateau.client.v2.ui.container;

import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Insets;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.core.Surface;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class TabComponent extends com.sypztep.plateau.client.v2.ui.widget.TabComponent {

    @Override public TabComponent tab(Component title, BaseComponent content) { super.tab(title, content); return this; }
    @Override public TabComponent tab(String title, BaseComponent content) { super.tab(title, content); return this; }
    @Override public TabComponent active(int index) { super.active(index); return this; }
    @Override public TabComponent headerHeight(int value) { super.headerHeight(value); return this; }
    @Override public TabComponent headerGap(int value) { super.headerGap(value); return this; }
    @Override public TabComponent contentGap(int value) { super.contentGap(value); return this; }
    @Override public TabComponent tabPaddingX(int value) { super.tabPaddingX(value); return this; }

    @Override public TabComponent padding(Insets padding) { super.padding(padding); return this; }
    @Override public TabComponent margins(Insets margins) { super.margins(margins); return this; }
    @Override public TabComponent surface(Surface surface) { super.surface(surface); return this; }
    @Override public TabComponent id(String id) { super.id(id); return this; }
    @Override public TabComponent visible(boolean visible) { super.visible(visible); return this; }
    @Override public TabComponent sizing(Sizing h, Sizing v) { super.sizing(h, v); return this; }
    @Override public TabComponent sizing(Sizing both) { super.sizing(both); return this; }
}
