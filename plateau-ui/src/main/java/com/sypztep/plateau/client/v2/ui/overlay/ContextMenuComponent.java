package com.sypztep.plateau.client.v2.ui.overlay;

import com.sypztep.plateau.client.v1.ui.core.UISounds;
import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Full-screen right-click context menu overlay.
 *
 * Add as the LAST child of your screen root. Open programmatically from a trigger's
 * {@code onRightClick} handler using {@link #openAt(double, double)} with
 * {@link BaseComponent#lastScreenMouseX()} / {@link BaseComponent#lastScreenMouseY()}.
 *
 * <pre>{@code
 *   ContextMenuComponent menu = Overlays.contextMenu()
 *       .label("Actions")
 *       .item("Copy",  () -> ...)
 *       .item("Paste", () -> ...)
 *       .separator()
 *       .checkItem("Word Wrap", false, checked -> ...);
 *
 *   LabelComponent trigger = WidgetComponents.label("Right-click me")
 *       .sizing(Sizing.fill(), Sizing.fixed(20))
 *       .onRightClick(e -> menu.openAt(BaseComponent.lastScreenMouseX(), BaseComponent.lastScreenMouseY()));
 *
 *   Containers.stack(Sizing.fill(), Sizing.fill())
 *       .child(content)   // contains trigger
 *       .child(menu);     // must be last
 * }</pre>
 */
@Environment(EnvType.CLIENT)
public class ContextMenuComponent extends BaseComponent<ContextMenuComponent> {

    // ── Item model ─────────────────────────────────────────────

    private enum ItemType { ACTION, SEPARATOR, LABEL, CHECK }

    private static final class MenuItem {
        final ItemType type;
        final @Nullable String text;
        final @Nullable String shortcut;
        boolean enabled;
        boolean checked;
        final @Nullable Runnable onAction;
        final @Nullable Consumer<Boolean> onToggle;

        private MenuItem(ItemType type, @Nullable String text, @Nullable String shortcut,
                         boolean enabled, boolean checked,
                         @Nullable Runnable onAction, @Nullable Consumer<Boolean> onToggle) {
            this.type = type; this.text = text; this.shortcut = shortcut;
            this.enabled = enabled; this.checked = checked;
            this.onAction = onAction; this.onToggle = onToggle;
        }

        static MenuItem action(String text, @Nullable String shortcut, boolean enabled, Runnable action) {
            return new MenuItem(ItemType.ACTION, text, shortcut, enabled, false, action, null);
        }
        static MenuItem separator() { return new MenuItem(ItemType.SEPARATOR, null, null, false, false, null, null); }
        static MenuItem label(String text) { return new MenuItem(ItemType.LABEL, text, null, false, false, null, null); }
        static MenuItem check(String text, boolean checked, Consumer<Boolean> onToggle) {
            return new MenuItem(ItemType.CHECK, text, null, true, checked, null, onToggle);
        }
    }

    // ── State ─────────────────────────────────────────────────

    private final List<MenuItem> items = new ArrayList<>();
    private boolean open = false;
    private float openProgress = 0f;
    private int menuX, menuY;
    private float[] rowHover = new float[0];

    // ── Layout constants ──────────────────────────────────────

    private static final int ITEM_H  = 14;
    private static final int SEP_H   = 5;
    private static final int LABEL_H = 11;
    private static final int PAD_X   = 8;
    private static final int MIN_W   = 120;

    public ContextMenuComponent() {
        this.horizontalSizing = Sizing.fill();
        this.verticalSizing   = Sizing.fill();
    }

    // ── Fluent API ────────────────────────────────────────────

    /** Add an enabled action item. */
    public ContextMenuComponent item(String text, Runnable action) {
        return item(text, null, true, action);
    }

    /** Add an action item with enabled flag. */
    public ContextMenuComponent item(String text, Runnable action, boolean enabled) {
        return item(text, null, enabled, action);
    }

    /** Add an action item with a keyboard shortcut hint (display only, not bound). */
    public ContextMenuComponent item(String text, @Nullable String shortcut, Runnable action) {
        return item(text, shortcut, true, action);
    }

    /** Add an action item with shortcut and enabled flag. */
    public ContextMenuComponent item(String text, @Nullable String shortcut, boolean enabled, Runnable action) {
        items.add(MenuItem.action(text, shortcut, enabled, action));
        return this;
    }

    /** Add a visual separator line. */
    public ContextMenuComponent separator() {
        items.add(MenuItem.separator());
        return this;
    }

    /** Add a non-interactive section header label. */
    public ContextMenuComponent label(String text) {
        items.add(MenuItem.label(text));
        return this;
    }

    /** Add a toggle item with a checkbox indicator. */
    public ContextMenuComponent checkItem(String text, boolean initialChecked, Consumer<Boolean> onChange) {
        items.add(MenuItem.check(text, initialChecked, onChange));
        return this;
    }

    /** Open the menu at the given screen coordinates (clamped to screen bounds). */
    public ContextMenuComponent openAt(double screenX, double screenY) {
        int mw = menuWidth(), mh = menuHeight();
        menuX = (int) Math.min(screenX, x + width  - mw - 2);
        menuY = (int) Math.min(screenY, y + height - mh - 2);
        menuX = Math.max(x + 2, menuX);
        menuY = Math.max(y + 2, menuY);
        open        = true;
        openProgress = 1f; // snap fully open — no fade-in delay
        return this;
    }

    public void close()     { open = false; }
    public boolean isOpen() { return open; }

    // ── Input behaviour ───────────────────────────────────────

    @Override
    protected boolean isFocusable() { return true; }

    @Override
    public @Nullable ComponentPath nextFocusPath(@NonNull FocusNavigationEvent event) {
        return null; // excluded from Tab navigation
    }

    @Override
    public boolean rendersAboveSiblings() { return open; }

    @Override
    public boolean blocksLowerInput() { return open; }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (open) {
            if (isOverMenu(event.x(), event.y())) {
                activateAt(event.y());
            } else {
                close();
            }
            return true; // always absorb while open
        }
        return false;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (!open) return false;
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        return open; // absorb scroll events while menu is showing
    }

    // ── Rendering ─────────────────────────────────────────────

    @Override
    public void extract(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        openProgress = stepAnimation(openProgress, open, 0.5f, delta);
        if (openProgress < 0.01f) return;

        UITheme theme = UITheme.current();
        int mw = menuWidth(), mh = menuHeight();
        ensureRowHoverArray();

        // Scale from the menu's top-left corner (where the cursor was)
        float scale = Mth.lerp(easeOut(openProgress), 0.85f, 1.0f);
        g.pose().pushMatrix();
        g.pose().translate(menuX, menuY);
        g.pose().scale(scale, scale);
        g.pose().translate(-menuX, -menuY);

        g.enableScissor(menuX, menuY, menuX + mw, menuY + mh);

        // Background
        g.fill(menuX + 1, menuY + 1, menuX + mw - 1, menuY + mh - 1, theme.panel().bg());
        g.outline(menuX, menuY, mw, mh, theme.panel().border());
        g.outline(menuX + 1, menuY + 1, mw - 2, mh - 2, theme.panel().border());

        // Items
        int iy = menuY + 2;
        for (int i = 0; i < items.size(); i++) {
            MenuItem item = items.get(i);
            boolean hov = open && isOverMenu(mouseX, mouseY) && rowAt(mouseY) == i && item.enabled;
            rowHover[i] = stepAnimation(rowHover[i], hov, 0.5f, delta);

            switch (item.type) {
                case ACTION -> {
                    if (item.text != null) {
                        int rowBg = ARGB.srgbLerp(rowHover[i], theme.panel().bg(), theme.panel().bgHover());
                        g.fill(menuX + 1, iy, menuX + mw - 1, iy + ITEM_H, rowBg);
                        int tc = item.enabled ? theme.text().primary() : theme.text().disabled();
                        g.text(font, item.text, menuX + PAD_X, iy + (ITEM_H - font.lineHeight) / 2, tc, false);
                        if (item.shortcut != null) {
                            int sx = menuX + mw - PAD_X - font.width(item.shortcut);
                            g.text(font, item.shortcut, sx, iy + (ITEM_H - font.lineHeight) / 2, theme.text().secondary(), false);
                        }
                    }
                    iy += ITEM_H;
                }
                case SEPARATOR -> {
                    g.fill(menuX + PAD_X, iy + SEP_H / 2, menuX + mw - PAD_X, iy + SEP_H / 2 + 1, theme.panel().border());
                    iy += SEP_H;
                }
                case LABEL -> {
                    if (item.text != null) {
                        g.text(font, item.text, menuX + PAD_X, iy + (LABEL_H - font.lineHeight) / 2, theme.text().secondary(), false);
                    }
                    iy += LABEL_H;
                }
                case CHECK -> {
                    if (item.text != null) {
                        int rowBg = ARGB.srgbLerp(rowHover[i], theme.panel().bg(), theme.panel().bgHover());
                        g.fill(menuX + 1, iy, menuX + mw - 1, iy + ITEM_H, rowBg);
                        // 7×7 checkbox
                        int cb = menuX + PAD_X, ct = iy + (ITEM_H - 7) / 2;
                        int checkBorder = item.checked ? theme.text().accent() : theme.panel().borderHover();
                        g.outline(cb, ct, 7, 7, checkBorder);
                        if (item.checked) g.fill(cb + 2, ct + 2, cb + 5, ct + 5, theme.text().accent());
                        int tc = item.enabled ? theme.text().primary() : theme.text().disabled();
                        g.text(font, item.text, menuX + PAD_X + 11, iy + (ITEM_H - font.lineHeight) / 2, tc, false);
                    }
                    iy += ITEM_H;
                }
            }
        }

        g.disableScissor();
        g.pose().popMatrix();
    }

    // ── Helpers ───────────────────────────────────────────────

    private void activateAt(double mouseY) {
        int row = rowAt(mouseY);
        if (row < 0 || row >= items.size()) return;
        MenuItem item = items.get(row);
        if (!item.enabled) return;
        if (item.type == ItemType.ACTION && item.onAction != null) {
            UISounds.playClick();
            item.onAction.run();
            close();
        } else if (item.type == ItemType.CHECK && item.onToggle != null) {
            UISounds.playClick();
            item.checked = !item.checked;
            item.onToggle.accept(item.checked);
            close();
        }
    }

    private boolean isOverMenu(double mx, double my) {
        return mx >= menuX && mx < menuX + menuWidth() && my >= menuY && my < menuY + menuHeight();
    }

    private int rowAt(double my) {
        int iy = menuY + 2;
        for (int i = 0; i < items.size(); i++) {
            int h = itemHeight(items.get(i));
            if (my >= iy && my < iy + h) return i;
            iy += h;
        }
        return -1;
    }

    private int itemHeight(MenuItem item) {
        return switch (item.type) {
            case ACTION, CHECK -> ITEM_H;
            case SEPARATOR -> SEP_H;
            case LABEL -> LABEL_H;
        };
    }

    private int menuWidth() {
        int w = MIN_W;
        for (MenuItem item : items) {
            if (item.text == null) continue;
            int tw = font.width(item.text) + PAD_X * 2;
            if (item.shortcut != null) tw += font.width(item.shortcut) + PAD_X * 2;
            if (item.type == ItemType.CHECK) tw += 11;
            w = Math.max(w, tw);
        }
        return w + 4; // +4 for border
    }

    private int menuHeight() {
        int h = 4; // top + bottom border padding
        for (MenuItem item : items) h += itemHeight(item);
        return h;
    }

    private void ensureRowHoverArray() {
        if (rowHover.length != items.size()) rowHover = new float[items.size()];
    }

    private static float easeOut(float t) { float inv = 1f - t; return 1f - inv * inv * inv; }
}
