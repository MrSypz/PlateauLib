package com.sypztep.plateau.client.impl.ui.widget;

import com.sypztep.plateau.client.impl.ui.core.UIComponent;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Vertical flex container. Positions children in a column with weight-based sizing.
 *
 * <pre>
 * UIColumn col = new UIColumn(x, y, width, totalHeight).gap(6);
 * col.add(topPanel, 0.3f);     // 30% height
 * col.add(bottomPanel, 0.7f);  // 70% height
 * </pre>
 */
public class UIColumn extends UIComponent implements ContainerEventHandler {
    private final List<Entry> entries = new ArrayList<>();
    private final List<GuiEventListener> children = new ArrayList<>();
    private int gap = 4;
    private Align align = Align.LEFT;
    private @Nullable GuiEventListener focused;
    private boolean isDragging;

    public enum Align { LEFT, CENTER, RIGHT }

    public UIColumn(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.focusable = false;
    }

    public UIColumn gap(int gap) { this.gap = gap; return this; }
    public UIColumn align(Align align) { this.align = align; return this; }

    public UIColumn add(UIComponent child, float weight) {
        entries.add(new Entry(child, weight));
        children.add(child);
        return this;
    }

    public UIColumn add(UIComponent child) {
        return add(child, 0f);
    }

    public void layout() {
        if (entries.isEmpty()) return;

        float totalWeight = 0;
        int fixedHeight = 0;
        int gapTotal = (entries.size() - 1) * gap;

        for (Entry e : entries) {
            if (e.weight > 0) {
                totalWeight += e.weight;
            } else {
                fixedHeight += e.child.getHeight();
            }
        }

        int flexSpace = height - fixedHeight - gapTotal;
        int cy = y;

        for (Entry e : entries) {
            int childH;
            if (e.weight > 0 && totalWeight > 0) {
                childH = (int)(flexSpace * (e.weight / totalWeight));
            } else {
                childH = e.child.getHeight();
            }

            int cx = switch (align) {
                case LEFT -> x;
                case CENTER -> x + (width - e.child.getWidth()) / 2;
                case RIGHT -> x + width - e.child.getWidth();
            };

            // For weighted children, set full width; for fixed, just position
            if (e.weight > 0) {
                e.child.setBounds(cx, cy, width, childH);
            } else {
                e.child.setPosition(cx, cy);
            }
            cy += childH + gap;
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
    // ContainerEventHandler
    // ═══════════════════════════════════════════

    @Override
    public @NonNull List<? extends GuiEventListener> children() {
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
    public @Nullable ComponentPath nextFocusPath(@NonNull FocusNavigationEvent event) {
        return ContainerEventHandler.super.nextFocusPath(event);
    }

    public List<UIComponent> getChildren() {
        return entries.stream().map(e -> e.child).toList();
    }

    private record Entry(UIComponent child, float weight) {}
}
