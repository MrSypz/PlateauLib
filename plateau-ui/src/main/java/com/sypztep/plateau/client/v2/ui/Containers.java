package com.sypztep.plateau.client.v2.ui;

import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.layout.FlowLayout;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Factory methods for v2 layout containers.
 * <pre>{@code
 * Containers.vertical(Sizing.fill(), Sizing.fill())
 *     .padding(Insets.of(8))
 *     .gap(4)
 *     .child(Components.label("Title"))
 *     .child(Components.button("Click", btn -> {}))
 * }</pre>
 */
@Environment(EnvType.CLIENT)
public final class Containers {

    private Containers() {}

    /** Vertical stack — children are arranged top-to-bottom. */
    public static FlowLayout vertical(Sizing horizontal, Sizing vertical) {
        return new FlowLayout(FlowLayout.Direction.VERTICAL, horizontal, vertical);
    }

    /** Horizontal row — children are arranged left-to-right. */
    public static FlowLayout horizontal(Sizing horizontal, Sizing vertical) {
        return new FlowLayout(FlowLayout.Direction.HORIZONTAL, horizontal, vertical);
    }

    /** Shorthand: vertical stack that fills its parent completely. */
    public static FlowLayout verticalFill() {
        return vertical(Sizing.fill(), Sizing.fill());
    }

    /** Shorthand: horizontal row that fills its parent completely. */
    public static FlowLayout horizontalFill() {
        return horizontal(Sizing.fill(), Sizing.fill());
    }

    /** Shorthand: vertical stack that fills width and shrinks to its content height. */
    public static FlowLayout verticalFlow() {
        return vertical(Sizing.fill(), Sizing.content());
    }
}
