package com.sypztep.plateau.client.v2.ui.layout;

import com.sypztep.plateau.client.v2.ui.core.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/**
 * A vertical or horizontal flex container. Children declare their size via {@link Sizing}:
 * <ul>
 *   <li>{@code Sizing.fixed(n)} — always n pixels</li>
 *   <li>{@code Sizing.fill()} — takes a proportional share of remaining space</li>
 *   <li>{@code Sizing.content()} — shrinks to fit its own content</li>
 * </ul>
 * Inherits event routing from {@link BaseContainerComponent}, so nested components
 * receive mouse and keyboard events without one-off forwarding in each layout.
 */
@Environment(EnvType.CLIENT)
public class FlowLayout extends BaseContainerComponent {

    public enum Direction { HORIZONTAL, VERTICAL }

    public enum Align { START, CENTER, END }

    private final Direction direction;
    private int gap = 0;
    private Align crossAlign = Align.START;

    public FlowLayout(Direction direction, Sizing horizontal, Sizing vertical) {
        this.direction        = direction;
        this.horizontalSizing = horizontal;
        this.verticalSizing   = vertical;
    }

    public FlowLayout gap(int gap)               { this.gap = gap; return this; }
    public FlowLayout crossAlign(Align align)    { this.crossAlign = align; return this; }

    // Override fluent base methods to preserve FlowLayout return type in chains
    @Override public FlowLayout padding(Insets padding)   { super.padding(padding);  return this; }
    @Override public FlowLayout margins(Insets margins)   { super.margins(margins);  return this; }
    @Override public FlowLayout surface(Surface surface)  { super.surface(surface);  return this; }
    @Override public FlowLayout id(String id)             { super.id(id);            return this; }
    @Override public FlowLayout visible(boolean visible)  { super.visible(visible);  return this; }
    @Override public FlowLayout sizing(Sizing h, Sizing v){ super.sizing(h, v);      return this; }
    @Override public FlowLayout sizing(Sizing both)       { super.sizing(both);      return this; }

    @Override public FlowLayout child(BaseComponent child)                             { super.child(child);         return this; }
    @Override public FlowLayout children(BaseComponent... components)                  { super.children(components); return this; }
    @Override public FlowLayout children(Iterable<? extends BaseComponent> components) { super.children(components); return this; }
    // ── PointerInteractable ───────────────────────────────────
    // ── Layout ───────────────────────────────────────────────

    @Override
    protected void onMounted() {
        layout();
    }

    private void layout() {
        if (direction == Direction.VERTICAL) {
            layoutVertical();
        } else {
            layoutHorizontal();
        }
    }

    private void layoutVertical() {
        int innerX = x + padding.left();
        int innerY = y + padding.top();
        int innerW = innerWidth();
        int innerH = innerHeight();

        List<BaseComponent> vis = visible();
        if (vis.isEmpty()) return;

        int totalGaps      = gap * (vis.size() - 1);
        int fixedH         = totalGaps;
        int totalFillWeight = 0;

        int[] heights = new int[vis.size()];
        int[] widths  = new int[vis.size()];

        for (int i = 0; i < vis.size(); i++) {
            BaseComponent c = vis.get(i);
            int childAvailW = Math.max(0, innerW - c.margins().horizontal());
            widths[i] = resolveWidth(c, childAvailW);

            switch (c.verticalSizing()) {
                case Sizing.Fixed  f -> { heights[i] = f.value(); fixedH += f.value() + c.margins().vertical(); }
                case Sizing.Content ignored -> {
                    heights[i] = c.determineVerticalContentSize(childAvailW);
                    fixedH += heights[i] + c.margins().vertical();
                }
                case Sizing.Fill   f -> totalFillWeight += Math.max(0, f.weight());
            }
        }

        int remaining  = Math.max(0, innerH - fixedH);
        float fillUnit = totalFillWeight > 0 ? (float) remaining / totalFillWeight : 0;

        for (int i = 0; i < vis.size(); i++) {
            if (vis.get(i).verticalSizing() instanceof Sizing.Fill(int weight)) {
                heights[i] = Math.max(0, (int)(Math.max(0, weight) * fillUnit));
            }
        }

        int curY = innerY;
        for (int i = 0; i < vis.size(); i++) {
            BaseComponent c = vis.get(i);
            int childX = resolveChildX(innerX, innerW, c, widths[i]);
            c.mount(childX, curY + c.margins().top(), widths[i], heights[i]);
            curY += heights[i] + c.margins().vertical() + (i < vis.size() - 1 ? gap : 0);
        }
    }

    private void layoutHorizontal() {
        int innerX = x + padding.left();
        int innerY = y + padding.top();
        int innerW = innerWidth();
        int innerH = innerHeight();

        List<BaseComponent> vis = visible();
        if (vis.isEmpty()) return;

        int totalGaps       = gap * (vis.size() - 1);
        int fixedW          = totalGaps;
        int totalFillWeight = 0;

        int[] widths  = new int[vis.size()];
        int[] heights = new int[vis.size()];

        for (int i = 0; i < vis.size(); i++) {
            BaseComponent c = vis.get(i);
            int childAvailH = Math.max(0, innerH - c.margins().vertical());
            int childAvailW = Math.max(0, innerW - c.margins().horizontal());
            heights[i] = resolveHeight(c, childAvailH, childAvailW);

            switch (c.horizontalSizing()) {
                case Sizing.Fixed  f -> { widths[i] = f.value(); fixedW += f.value() + c.margins().horizontal(); }
                case Sizing.Content ignored -> {
                    widths[i] = c.determineHorizontalContentSize(childAvailW);
                    fixedW += widths[i] + c.margins().horizontal();
                }
                case Sizing.Fill   f -> totalFillWeight += Math.max(0, f.weight());
            }
        }

        int remaining  = Math.max(0, innerW - fixedW);
        float fillUnit = totalFillWeight > 0 ? (float) remaining / totalFillWeight : 0;

        for (int i = 0; i < vis.size(); i++) {
            if (vis.get(i).horizontalSizing() instanceof Sizing.Fill(int weight)) {
                widths[i] = Math.max(0, (int)(Math.max(0, weight) * fillUnit));
            }
        }

        int curX = innerX;
        for (int i = 0; i < vis.size(); i++) {
            BaseComponent c = vis.get(i);
            int childY = resolveChildY(innerY, innerH, c, heights[i]);
            c.mount(curX + c.margins().left(), childY, widths[i], heights[i]);
            curX += widths[i] + c.margins().horizontal() + (i < vis.size() - 1 ? gap : 0);
        }
    }

    // ── Sizing helpers ────────────────────────────────────────

    private int resolveWidth(BaseComponent c, int availW) {
        return switch (c.horizontalSizing()) {
            case Sizing.Fixed   f -> f.value();
            case Sizing.Fill    ignored -> availW;
            case Sizing.Content ignored -> c.determineHorizontalContentSize(availW);
        };
    }

    private int resolveHeight(BaseComponent c, int availH, int availW) {
        return switch (c.verticalSizing()) {
            case Sizing.Fixed   f -> f.value();
            case Sizing.Fill    ignored -> availH;
            case Sizing.Content ignored -> c.determineVerticalContentSize(availW);
        };
    }

    private int resolveChildX(int innerX, int innerW, BaseComponent c, int childW) {
        int margin = c.margins().left();
        return switch (crossAlign) {
            case START  -> innerX + margin;
            case CENTER -> innerX + (innerW - childW - c.margins().horizontal()) / 2 + margin;
            case END    -> innerX + innerW - childW - c.margins().right();
        };
    }

    private int resolveChildY(int innerY, int innerH, BaseComponent c, int childH) {
        int margin = c.margins().top();
        return switch (crossAlign) {
            case START  -> innerY + margin;
            case CENTER -> innerY + (innerH - childH - c.margins().vertical()) / 2 + margin;
            case END    -> innerY + innerH - childH - c.margins().bottom();
        };
    }

    private List<BaseComponent> visible() {
        return children.stream().filter(BaseComponent::isVisible).toList();
    }

    // ── Content size (for Sizing.content() on this FlowLayout) ─

    @Override
    public int determineVerticalContentSize(int availableWidth) {
        int innerW = Math.max(0, availableWidth - padding.horizontal());
        List<BaseComponent> vis = visible();
        if (vis.isEmpty()) return padding.vertical();

        if (direction == Direction.VERTICAL) {
            int total = padding.vertical() + gap * Math.max(0, vis.size() - 1);
            for (BaseComponent c : vis) {
                int childAvailW = Math.max(0, innerW - c.margins().horizontal());
                total += childContentH(c, childAvailW) + c.margins().vertical();
            }
            return total;
        } else {
            int maxH = 0;
            for (BaseComponent c : vis) {
                int childAvailW = Math.max(0, innerW - c.margins().horizontal());
                maxH = Math.max(maxH, childContentH(c, childAvailW) + c.margins().vertical());
            }
            return maxH + padding.vertical();
        }
    }

    @Override
    public int determineHorizontalContentSize(int availableWidth) {
        int innerW = Math.max(0, availableWidth - padding.horizontal());
        List<BaseComponent> vis = visible();
        if (vis.isEmpty()) return padding.horizontal();

        if (direction == Direction.HORIZONTAL) {
            int total = padding.horizontal() + gap * Math.max(0, vis.size() - 1);
            for (BaseComponent c : vis) {
                int childAvailW = Math.max(0, innerW - c.margins().horizontal());
                total += childContentW(c, childAvailW) + c.margins().horizontal();
            }
            return total;
        } else {
            int maxW = 0;
            for (BaseComponent c : vis) {
                int childAvailW = Math.max(0, innerW - c.margins().horizontal());
                maxW = Math.max(maxW, childContentW(c, childAvailW) + c.margins().horizontal());
            }
            return maxW + padding.horizontal();
        }
    }

    private static int childContentH(BaseComponent c, int availW) {
        return switch (c.verticalSizing()) {
            case Sizing.Fixed   f -> f.value();
            case Sizing.Content ignored -> c.determineVerticalContentSize(availW);
            case Sizing.Fill    ignored -> 0;
        };
    }

    private static int childContentW(BaseComponent c, int availW) {
        return switch (c.horizontalSizing()) {
            case Sizing.Fixed   f -> f.value();
            case Sizing.Content ignored -> c.determineHorizontalContentSize(availW);
            case Sizing.Fill    ignored -> 0;
        };
    }

    // ── Rendering ─────────────────────────────────────────────

    @Override
    public void extract(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (width <= 0 || height <= 0) return;

        g.enableScissor(x, y, x + width, y + height);
        for (BaseComponent child : children) {
            if (child.isVisible()) {
                child.extractRenderState(g, mouseX, mouseY, delta);
            }
        }
        g.disableScissor();
    }
}
