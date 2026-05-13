package com.sypztep.plateau.client.v2.ui.container;

import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Insets;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.core.Surface;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class ScrollablePanel extends PanelComponent {
    private final ScrollContainer scrollBody = new ScrollContainer(Sizing.fill(), Sizing.fill())
            .gap(3)
            .padding(Insets.of(4))
            .surface(Surface.NONE);

    public ScrollablePanel(Component title) {
        super(title);
        verticalSizing = Sizing.fill();
        super.child(scrollBody);
    }

    public ScrollablePanel(String title) {
        this(Component.literal(title));
    }

    @Override
    public ScrollablePanel child(BaseComponent<?> child) {
        scrollBody.child(child);
        return this;
    }

    @Override
    public ScrollablePanel children(BaseComponent<?>... components) {
        scrollBody.children(components);
        return this;
    }

    @Override
    public ScrollablePanel children(Iterable<? extends BaseComponent<?>> components) {
        scrollBody.children(components);
        return this;
    }

    @Override
    public ScrollablePanel gap(int gap) {
        scrollBody.gap(gap);
        return this;
    }

    public ScrollablePanel scrollPadding(Insets padding) {
        scrollBody.padding(padding);
        return this;
    }

    public ScrollContainer scrollBody() {
        return scrollBody;
    }
}
