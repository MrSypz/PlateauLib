package com.sypztep.plateau.client.impl.ui.widget;

import com.sypztep.plateau.client.impl.ui.core.UIComponent;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal flex container. Positions children in a row with weight-based sizing.
 *
 * <pre>
 * UIRow row = new UIRow(x, y, totalWidth, height).gap(6);
 * row.add(leftPanel, 0.4f);   // 40% width
 * row.add(rightPanel, 0.6f);  // 60% width
 * </pre>
 */
public class UIRow extends UIComponent implements ContainerEventHandler {
    private final List<Entry> entries = new ArrayList<>();
    private final List<GuiEventListener> children = new ArrayList<>();
    private int gap = 4;
    private @Nullable GuiEventListener focused;
    private boolean isDragging;

    public UIRow(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.focusable = false; // container delegates focus to children
    }

    public UIRow gap(int gap) { this.gap = gap; return this; }

    public UIRow add(UIComponent child, float weight) {
        entries.add(new Entry(child, weight));
        children.add(child);
        return this;
    }

    public UIRow add(UIComponent child) {
        return add(child, 0f); // 0 weight = use child's natural width
    }

    /**
     * Recalculate child positions and sizes. Called automatically during render,
     * but can be called manually after changing bounds.
     */
    public void layout() {
        if (entries.isEmpty()) return;

        float totalWeight = 0;
        int fixedWidth = 0;
        int gapTotal = (entries.size() - 1) * gap;

        for (Entry e : entries) {
            if (e.weight > 0) {
                totalWeight += e.weight;
            } else {
                fixedWidth += e.child.getWidth();
            }
        }

        int flexSpace = width - fixedWidth - gapTotal;
        int cx = x;

        for (Entry e : entries) {
            int childW;
            if (e.weight > 0 && totalWeight > 0) {
                childW = (int)(flexSpace * (e.weight / totalWeight));
            } else {
                childW = e.child.getWidth();
            }
            e.child.setBounds(cx, y, childW, height);
            cx += childW + gap;
        }
    }

    @Override
    protected void renderComponent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        layout();
        for (Entry e : entries) {
            e.child.extractRenderState(graphics, mouseX, mouseY, delta);
        }
    }

    // ═══════════════════════════════════════════
    // ContainerEventHandler — delegates focus to children
    // ═══════════════════════════════════════════

    @Override
    public List<? extends GuiEventListener> children() {
        return children;
    }

    @Override
    public @Nullable GuiEventListener getFocused() { return focused; }

    @Override
    public void setFocused(@Nullable GuiEventListener listener) { this.focused = listener; }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused && this.focused != null) {
            this.focused.setFocused(false);
            this.focused = null;
        }
    }

    @Override
    public boolean isDragging() { return isDragging; }

    @Override
    public void setDragging(boolean dragging) { this.isDragging = dragging; }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        return ContainerEventHandler.super.nextFocusPath(event);
    }

    public List<UIComponent> getChildren() {
        return entries.stream().map(e -> e.child).toList();
    }

    private record Entry(UIComponent child, float weight) {}
}
