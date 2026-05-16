package com.sypztep.plateau.client.v2.ui.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Base for components that expand beyond their layout bounds (dropdowns, comboboxes, etc.).
 * Subclasses declare how much extra vertical space the expanded state needs via
 * {@link #expandedExtra()} and call {@link #setExpanded}/{@link #isExpanded} — the engine
 * boilerplate (rendersAboveSiblings, renderClipBottomOutset, blocksLowerInput, hitTest)
 * is handled here automatically.
 *
 * <p>The expanded region is assumed to sit directly below the component's layout bounds.
 * Override {@link #isInExpandedArea} if your geometry differs.
 */
@Environment(EnvType.CLIENT)
public abstract class BaseExpandingComponent<S extends BaseExpandingComponent<S>>
        extends BaseComponent<S> {

    private boolean expanded = false;

    /** Pixels of extra height needed below this component's layout bounds when expanded. */
    protected abstract int expandedExtra();

    protected void setExpanded(boolean v) { this.expanded = v; }
    protected boolean isExpanded()        { return expanded; }

    @Override public boolean rendersAboveSiblings()  { return expanded; }
    @Override public boolean blocksLowerInput()       { return expanded; }
    @Override public int    renderClipBottomOutset() { return expanded ? expandedExtra() : 0; }

    @Override
    public boolean hitTest(double mouseX, double mouseY) {
        if (!visible) return false;
        return super.hitTest(mouseX, mouseY)
                || (expanded && isInExpandedArea(mouseX, mouseY));
    }

    /** Returns true if (mouseX, mouseY) is inside the expanded region below this component. */
    protected boolean isInExpandedArea(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width
                && mouseY >= y + height && mouseY < y + height + expandedExtra();
    }
}
