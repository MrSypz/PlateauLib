package com.sypztep.plateau.client.ui.layout;

import com.sypztep.plateau.client.ui.core.UIComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Positions components in a horizontal row.
 */
public class RowLayout {
    private final int x, y, height;
    private int gap = 4;
    private final List<UIComponent> components = new ArrayList<>();

    public RowLayout(int x, int y, int height) {
        this.x = x;
        this.y = y;
        this.height = height;
    }

    public RowLayout gap(int gap) { this.gap = gap; return this; }

    public RowLayout add(UIComponent component) {
        components.add(component);
        return this;
    }

    /**
     * Compute and set positions. Returns total width used.
     */
    public int apply() {
        int cx = x;
        for (UIComponent c : components) {
            // Vertically center within row height
            int cy = y + (height - c.getHeight()) / 2;
            c.setPosition(cx, cy);
            cx += c.getWidth() + gap;
        }
        return cx - x - (components.isEmpty() ? 0 : gap);
    }

    public List<UIComponent> getComponents() { return components; }
}