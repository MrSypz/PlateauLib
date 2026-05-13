package com.sypztep.plateau.client.v2.ui;

import com.sypztep.plateau.client.v2.ui.container.ScrollContainer;
import com.sypztep.plateau.client.v2.ui.container.PanelComponent;
import com.sypztep.plateau.client.v2.ui.container.ScrollablePanel;
import com.sypztep.plateau.client.v2.ui.core.Insets;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.core.Surface;
import com.sypztep.plateau.client.v2.ui.overlay.DetachablePanel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

/** High-level panel factories for fixed, scrollable, and detachable dashboard regions. */
@Environment(EnvType.CLIENT)
public final class Panels {
    private Panels() {}

    public static PanelComponent fixed(String title) {
        return fixed(Component.literal(title));
    }

    public static PanelComponent fixed(Component title) {
        return new PanelComponent(title);
    }

    public static ScrollablePanel scroll(String title) {
        return scroll(Component.literal(title));
    }

    public static ScrollablePanel scroll(Component title) {
        return new ScrollablePanel(title);
    }

    public static ScrollContainer scrollBody() {
        return Containers.scrollable(Sizing.fill(), Sizing.fill())
                .gap(3)
                .padding(Insets.of(4))
                .surface(Surface.outline());
    }

    public static DetachablePanel detachable(String title) {
        return detachable(Component.literal(title));
    }

    public static DetachablePanel detachable(Component title) {
        return new DetachablePanel(title);
    }
}
