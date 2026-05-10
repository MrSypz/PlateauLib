package com.sypztep.plateau.client.v2.ui.core;

import com.sypztep.plateau.client.v2.ui.widget.ScrollContainer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * Implemented by v2 components that need to receive pointer (mouse) events with
 * content-space coordinates. A scroll-aware parent (e.g. {@link ScrollContainer})
 * computes the scroll offset once and passes adjusted coordinates so that the
 * rendered position and the hit-test position always agree.
 *
 * <p>Without this interface, {@code isMouseOver} compares screen-space mouse Y
 * against layout-space child Y, which only matches when scroll offset is zero.
 */
@Environment(EnvType.CLIENT)
public interface PointerInteractable {

    /**
     * Returns true if the content-space point {@code (x, y)} falls inside this
     * component's bounds. Callers have already added the scroll offset, so
     * {@code y} can be compared directly against layout coordinates.
     */
    boolean hitTest(double x, double y);

    /**
     * Called by a scroll-aware parent when a mouse button is pressed.
     * {@code x} and {@code y} are content-space coordinates — the caller has
     * added the scroll offset so they match the layout positions used by children.
     *
     * @return true if the event was consumed
     */
    boolean onPointerClicked(MouseButtonEvent event, boolean doubleClick, double x, double y);
}
