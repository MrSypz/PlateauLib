package com.sypztep.plateau.client.v2.ui.container;

import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Insets;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.core.Surface;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ScrollContainer extends com.sypztep.plateau.client.v2.ui.widget.ScrollContainer {

    public ScrollContainer(Sizing horizontal, Sizing vertical) {
        super(horizontal, vertical);
    }

    @Override public ScrollContainer child(BaseComponent child) { super.child(child); return this; }
    @Override public ScrollContainer children(BaseComponent... components) { super.children(components); return this; }
    @Override public ScrollContainer children(Iterable<? extends BaseComponent> components) { super.children(components); return this; }
    @Override public ScrollContainer gap(int gap) { super.gap(gap); return this; }

    @Override public ScrollContainer padding(Insets padding) { super.padding(padding); return this; }
    @Override public ScrollContainer margins(Insets margins) { super.margins(margins); return this; }
    @Override public ScrollContainer surface(Surface surface) { super.surface(surface); return this; }
    @Override public ScrollContainer id(String id) { super.id(id); return this; }
    @Override public ScrollContainer visible(boolean visible) { super.visible(visible); return this; }
    @Override public ScrollContainer sizing(Sizing h, Sizing v) { super.sizing(h, v); return this; }
    @Override public ScrollContainer sizing(Sizing both) { super.sizing(both); return this; }
}
