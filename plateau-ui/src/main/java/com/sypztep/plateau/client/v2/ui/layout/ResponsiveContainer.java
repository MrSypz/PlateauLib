package com.sypztep.plateau.client.v2.ui.layout;

import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.BaseContainerComponent;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.IntFunction;

/**
 * Swaps its entire child subtree based on the width it is offered, rebuilt fresh every time this
 * container is (re)mounted (e.g. window resize, tab switch) — not per frame.
 *
 * Unlike FlowLayout/Row/Column, which reflow the SAME children in place, this discards the whole
 * previous subtree and builds a new one from {@code layoutBuilder}, similar to a React key change
 * forcing an unmount + remount instead of a re-render. Any state living in the discarded subtree
 * (scroll position, in-progress text input) is lost when the width crosses a value where your
 * builder returns a different tree — this is an explicit escape hatch for restructuring the whole
 * panel tree by available width, not a substitute for in-place reflow.
 *
 * <pre>{@code
 *   Containers.responsive(Sizing.fill(), Sizing.fill())
 *       .breakpoint(width -> width < 500
 *           ? compactLayout()   // e.g. stack panels vertically, or fold into a TabComponent
 *           : wideLayout());    // normal side-by-side arrangement
 * }</pre>
 */
@Environment(EnvType.CLIENT)
public class ResponsiveContainer extends BaseContainerComponent<ResponsiveContainer> {

    private IntFunction<BaseComponent<?>> layoutBuilder = width -> null;
    private int builtForWidth = Integer.MIN_VALUE;

    public ResponsiveContainer(Sizing horizontal, Sizing vertical) {
        this.horizontalSizing = horizontal;
        this.verticalSizing = vertical;
    }

    /**
     * {@code layoutBuilder} receives the width offered to this container every time it is
     * (re)mounted; its return value becomes this container's entire child subtree. Compare the
     * width yourself to choose a layout, e.g. {@code width -> width < 500 ? compact() : wide()}.
     * Not re-invoked on a remount that offers the same width as last time, so state survives
     * layout passes that don't actually change this container's width.
     */
    public ResponsiveContainer breakpoint(IntFunction<BaseComponent<?>> layoutBuilder) {
        this.layoutBuilder = layoutBuilder == null ? width -> null : layoutBuilder;
        builtForWidth = Integer.MIN_VALUE; // force a rebuild on the next mount
        return this;
    }

    @Override
    protected void onMounted() {
        rebuildIfNeeded(innerWidth());

        BaseComponent<?> current = current();
        if (current != null) {
            current.mount(innerX(), innerY(), innerWidth(), innerHeight());
        }
    }

    @Override
    public void extract(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        extractChildrenInLayerOrder(g, mouseX, mouseY, delta);
    }

    private void rebuildIfNeeded(int forWidth) {
        if (forWidth == builtForWidth && !children.isEmpty()) return;
        builtForWidth = forWidth;

        children.clear();
        setFocused(null);
        BaseComponent<?> built = layoutBuilder.apply(forWidth);
        if (built != null) children.add(built);
    }

    private BaseComponent<?> current() {
        return children.isEmpty() ? null : children.get(0);
    }
}
