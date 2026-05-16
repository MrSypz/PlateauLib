package com.sypztep.plateau.client.v2.ui.widget;

import com.sypztep.plateau.client.v1.ui.core.RenderHelper;
import com.sypztep.plateau.client.v1.ui.core.UISounds;
import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.WidgetComponents;
import com.sypztep.plateau.client.v2.ui.core.BaseExpandingComponent;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Same look and feel as DropdownComponent, with a StringComponent search field
 * added at the top of the expanded list to filter items by typing.
 */
@Environment(EnvType.CLIENT)
public class SearchableDropdownComponent<T> extends BaseExpandingComponent<SearchableDropdownComponent<T>> {

    private static final int SEARCH_H  = 16;
    private static final int SEARCH_GAP = 1;
    private static final int MAX_ROWS  = 6;

    private final List<T>             allValues;
    private final Function<T, String> labeler;
    private final StringComponent     search;

    private List<T>           filtered;
    private @Nullable T       selectedValue;
    private @Nullable Consumer<T> onChanged;
    private @Nullable BiPredicate<T, String> filterPredicate = null;
    private boolean           enabled = true;

    private float   hoverProgress = 0f;
    private float   openProgress  = 0f;
    private boolean wasHovered    = false;
    private float[] rowHover      = new float[0];
    private boolean[] rowWasHov   = new boolean[0];

    @Override protected boolean isFocusable() { return enabled; }

    @Override
    protected int expandedExtra() {
        return SEARCH_H + SEARCH_GAP + Math.min(filtered.size(), MAX_ROWS) * height + 2;
    }

    public SearchableDropdownComponent(List<T> values, Function<T, String> labeler) {
        this.allValues = List.copyOf(values);
        this.labeler   = labeler;
        this.filtered  = this.allValues;
        this.search    = WidgetComponents.string("Search…")
                .maxLength(80)
                .onChanged(this::filter)
                .sizing(Sizing.fill(), Sizing.fixed(SEARCH_H));
        this.horizontalSizing = Sizing.fill();
        this.verticalSizing   = Sizing.fixed(20);
    }

    // ── Fluent ────────────────────────────────────────────────────

    public SearchableDropdownComponent<T> value(@Nullable T v) {
        this.selectedValue = v;
        return self();
    }

    public SearchableDropdownComponent<T> onChanged(Consumer<T> c) {
        this.onChanged = c;
        return self();
    }

    public @Nullable T      value()              { return selectedValue; }
    public SearchableDropdownComponent<T> enabled(boolean v) { this.enabled = v; return self(); }

    /**
     * Provide a custom filter predicate: {@code (item, rawQuery) -> matches}.
     * When set, this replaces the default label-contains check entirely —
     * the predicate receives the raw (un-lowercased) query string so it can
     * implement special syntax such as {@code #tag:path} lookups.
     */
    public SearchableDropdownComponent<T> filterPredicate(BiPredicate<T, String> predicate) {
        this.filterPredicate = predicate;
        return self();
    }

    // ── Rendering ─────────────────────────────────────────────────

    @Override
    public void extract(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (width <= 0 || height <= 0) return;

        UITheme theme = UITheme.current();
        boolean hot = enabled && super.hitTest(mouseX, mouseY);
        if (hot && !wasHovered) UISounds.playHover();
        wasHovered    = hot;
        hoverProgress = stepAnimation(hoverProgress, enabled && (hot || focused), 0.5f, delta);
        openProgress  = stepAnimation(openProgress, isExpanded(), 0.45f, delta);

        // Trigger row — identical to DropdownComponent
        drawTrigger(g, triggerLabel(), hoverProgress);

        int arrowX = innerX() + innerWidth() - 12;
        int arrowY = innerY() + innerHeight() / 2 - 2;
        int arrowColor = enabled ? theme.text().primary() : theme.text().disabled();
        if (isExpanded()) {
            g.fill(arrowX, arrowY + 3, arrowX + 7, arrowY + 4, arrowColor);
            g.fill(arrowX + 1, arrowY + 2, arrowX + 6, arrowY + 3, arrowColor);
            g.fill(arrowX + 2, arrowY + 1, arrowX + 5, arrowY + 2, arrowColor);
        } else {
            g.fill(arrowX, arrowY, arrowX + 7, arrowY + 1, arrowColor);
            g.fill(arrowX + 1, arrowY + 1, arrowX + 6, arrowY + 2, arrowColor);
            g.fill(arrowX + 2, arrowY + 2, arrowX + 5, arrowY + 3, arrowColor);
        }

        if (openProgress <= 0f && !isExpanded()) return;

        // Search field
        search.extractRenderState(g, mouseX, mouseY, delta);

        // Filtered list rows
        int visibleCount = Math.min(filtered.size(), MAX_ROWS);
        if (rowHover.length != visibleCount) {
            rowHover  = new float[visibleCount];
            rowWasHov = new boolean[visibleCount];
        }

        int listStartY = y + height + SEARCH_H + SEARCH_GAP;
        for (int i = 0; i < visibleCount; i++) {
            int rowY   = listStartY + i * height;
            boolean rh = isExpanded() && mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + height;
            if (rh && !rowWasHov[i]) UISounds.playHover();
            rowWasHov[i] = rh;
            rowHover[i]  = stepAnimation(rowHover[i], rh, 0.5f, delta);
            drawListRow(g, Component.literal(labeler.apply(filtered.get(i))), rowY, rowHover[i],
                    filtered.get(i).equals(selectedValue), openProgress);
        }
    }

    private void drawTrigger(GuiGraphicsExtractor g, Component label, float hover) {
        RenderHelper.ButtonColors colors = RenderHelper.buttonColors(enabled, hover, 0f);
        g.fillGradient(x + 2, y + 2, x + width - 2, y + height - 4, colors.bg(), colors.bgTop());
        g.outline(x, y, width, height, colors.border());
        g.outline(x + 1, y + 1, width - 2, height - 4, colors.outline());
        g.fill(x + 1, y + height - 3, x + width - 1, y + height - 1, colors.underline());
        int cx = innerX() + 5, cy = y + padding.top();
        int cw = Math.max(0, innerWidth() - 21), ch = Math.max(0, height - padding.vertical());
        g.enableScissor(cx, y, cx + cw, y + height);
        g.text(font, label, cx, cy + (ch - font.lineHeight) / 2, colors.text(), true);
        g.disableScissor();
    }

    private void drawListRow(GuiGraphicsExtractor g, Component label, int rowY, float hover, boolean selected, float prog) {
        UITheme theme = UITheme.current();
        int alpha   = (int)(prog * 0xFF) << 24;
        int baseBg  = (theme.panel().bg()      & 0x00FFFFFF) | alpha;
        int hoverBg = (theme.panel().bgHover() & 0x00FFFFFF) | alpha;
        g.fill(x, rowY, x + width, rowY + height, ARGB.srgbLerp(hover, baseBg, hoverBg));
        if (selected) g.fill(x, rowY, x + 2, rowY + height, (theme.panel().borderHover() & 0x00FFFFFF) | alpha);
        g.fill(x + 4, rowY + height - 1, x + width - 4, rowY + height,
                (theme.panel().border() & 0x00FFFFFF) | (int)(prog * 0x55) << 24);
        int textColor = enabled
                ? ARGB.srgbLerp(prog, theme.text().disabled(), selected ? theme.text().accent() : theme.text().primary())
                : theme.text().disabled();
        int cx = innerX() + (selected ? 9 : 5), cy = rowY + padding.top();
        int ch = Math.max(0, height - padding.vertical());
        g.enableScissor(x, rowY, x + width, rowY + height);
        g.text(font, label, cx, cy + (ch - font.lineHeight) / 2, textColor, true);
        g.disableScissor();
    }

    // ── Input ─────────────────────────────────────────────────────

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) { return hitTest(mouseX, mouseY); }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!enabled || event.button() != 0) return false;
        double mx = event.x(), my = event.y();

        if (isExpanded()) {
            // Click in search field
            int searchY = y + height;
            if (mx >= x && mx < x + width && my >= searchY && my < searchY + SEARCH_H) {
                search.mouseClicked(event, doubleClick);
                search.setFocused(true);
                return true;
            }
            // Click in list rows
            int listY = y + height + SEARCH_H + SEARCH_GAP;
            int visibleCount = Math.min(filtered.size(), MAX_ROWS);
            if (mx >= x && mx < x + width && my >= listY && my < listY + visibleCount * height) {
                int i = (int)((my - listY) / height);
                if (i >= 0 && i < visibleCount) select(filtered.get(i));
                UISounds.playClick();
                return true;
            }
            // Click trigger → toggle closed
            if (super.hitTest(mx, my)) { collapse(); UISounds.playClick(); return true; }
            // Click outside → close
            collapse();
            return false;
        }

        if (!super.hitTest(mx, my)) return false;
        expand();
        UISounds.playClick();
        return true;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (isExpanded()) search.mouseReleased(event);
        return false;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (!isExpanded()) return false;
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { collapse(); return true; }
        return search.keyPressed(event);
    }

    @Override
    public boolean keyReleased(@NonNull KeyEvent event) {
        return isExpanded() && search.keyReleased(event);
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        return isExpanded() && search.charTyped(event);
    }

    @Override
    public boolean preeditUpdated(@Nullable PreeditEvent event) {
        return isExpanded() && search.preeditUpdated(event);
    }

    // ── Internal ──────────────────────────────────────────────────

    private Component triggerLabel() {
        return selectedValue != null
                ? Component.literal(labeler.apply(selectedValue))
                : Component.literal("─ none ─");
    }

    private void filter(String query) {
        if (filterPredicate != null) {
            filtered = allValues.stream()
                    .filter(v -> filterPredicate.test(v, query))
                    .toList();
        } else {
            String lower = query.toLowerCase(Locale.ROOT);
            filtered = allValues.stream()
                    .filter(v -> labeler.apply(v).toLowerCase(Locale.ROOT).contains(lower))
                    .toList();
        }
        rowHover  = new float[Math.min(filtered.size(), MAX_ROWS)];
        rowWasHov = new boolean[Math.min(filtered.size(), MAX_ROWS)];
    }

    private void expand() {
        setExpanded(true);
        search.value("");
        filtered = allValues;
        search.mount(x, y + height, width, SEARCH_H);
        search.setFocused(true);
    }

    private void collapse() {
        setExpanded(false);
        search.setFocused(false);
        search.value("");
        filtered = allValues;
    }

    private void select(T v) {
        selectedValue = v;
        collapse();
        if (onChanged != null) onChanged.accept(v);
    }
}
