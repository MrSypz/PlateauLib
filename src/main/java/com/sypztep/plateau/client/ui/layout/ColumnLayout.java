package com.sypztep.plateau.client.ui.layout;

import com.sypztep.plateau.client.ui.core.UIComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Positions a list of components in a vertical column.
 * Call {@link #apply()} after adding all components to set their positions.
 *
 * Not a container — doesn't own or render the components.
 * Just computes and sets x/y on each one.
 */
public class ColumnLayout {
    private final int x, y, width;
    private int gap = 4;
    private Align align = Align.LEFT;
    private final List<UIComponent> components = new ArrayList<>();

    public enum Align { LEFT, CENTER, RIGHT }

    public ColumnLayout(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public ColumnLayout gap(int gap) { this.gap = gap; return this; }
    public ColumnLayout align(Align align) { this.align = align; return this; }

    public ColumnLayout add(UIComponent component) {
        components.add(component);
        return this;
    }

    /**
     * Compute and set positions on all added components.
     * Returns total height used (useful for sizing a parent panel).
     */
    public int apply() {
        int cy = y;
        for (UIComponent c : components) {
            int cx = switch (align) {
                case LEFT -> x;
                case CENTER -> x + (width - c.getWidth()) / 2;
                case RIGHT -> x + width - c.getWidth();
            };
            c.setPosition(cx, cy);
            cy += c.getHeight() + gap;
        }
        return cy - y - (components.isEmpty() ? 0 : gap);
    }

    public List<UIComponent> getComponents() { return components; }
}