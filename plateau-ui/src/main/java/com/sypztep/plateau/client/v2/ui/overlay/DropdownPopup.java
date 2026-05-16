package com.sypztep.plateau.client.v2.ui.overlay;

import com.sypztep.plateau.client.v1.ui.core.RenderHelper;
import com.sypztep.plateau.client.v1.ui.core.UISounds;
import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.Containers;
import com.sypztep.plateau.client.v2.ui.WidgetComponents;
import com.sypztep.plateau.client.v2.ui.container.ScrollContainer;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Insets;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.widget.StringComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Searchable dropdown popup — a root-level overlay (add as last child of your
 * StackLayout root). Position it by calling {@link #openAt} from a trigger's
 * onClick handler, passing {@link BaseComponent#currentScreenMouseX()} /
 * {@link BaseComponent#currentScreenMouseY()} or a fixed anchor position.
 *
 * <p>Because it lives at the root level it uses screen-space coordinates throughout,
 * so there are zero coordinate-space or scissor issues regardless of how deep the
 * trigger is nested.
 *
 * <pre>{@code
 *   // 1. Create the popup (generic over T)
 *   DropdownPopup<String> popup = new DropdownPopup<>(values, s -> s)
 *       .onSelect(v -> label.text(Component.literal(v)));
 *
 *   // 2. Create any trigger — just a normal button
 *   ButtonComponent trigger = WidgetComponents.button("Choose…")
 *       .onClick(b -> popup.openBelow(b.x(), b.y() + b.height(), b.width()));
 *
 *   // 3. Both go in the layout. Popup must be last.
 *   Containers.stack(Sizing.fill(), Sizing.fill())
 *       .child(content)   // contains trigger
 *       .child(popup);    // last
 * }</pre>
 */
@Environment(EnvType.CLIENT)
public class DropdownPopup<T> extends BaseComponent<DropdownPopup<T>> {

    private static final int SEARCH_H = 16;
    private static final int LIST_H   = 96;
    private static final int PAD      = 2;
    private static final int TOTAL_H  = SEARCH_H + PAD + LIST_H + PAD * 2;
    private static final int MIN_W    = 120;

    private final List<T>             allValues;
    private final Function<T, String> labeler;
    private final StringComponent     searchField;
    private ScrollContainer           listBody;

    private @Nullable Consumer<T> onSelect;
    private @Nullable T           selectedValue;

    private boolean open        = false;
    private int     popX, popY, popW;

    public DropdownPopup(List<T> values, Function<T, String> labeler) {
        this.allValues   = List.copyOf(values);
        this.labeler     = labeler;
        this.searchField = WidgetComponents.string("Search…")
                .maxLength(80)
                .onChanged(this::rebuildList)
                .sizing(Sizing.fill(), Sizing.fixed(SEARCH_H));
        rebuildList("");
        this.horizontalSizing = Sizing.fill();
        this.verticalSizing   = Sizing.fill();
    }

    // ── Fluent API ────────────────────────────────────────────

    public DropdownPopup<T> onSelect(Consumer<T> handler) { this.onSelect = handler; return this; }
    public DropdownPopup<T> value(@Nullable T v)          { this.selectedValue = v; return this; }
    public @Nullable T      value()                       { return selectedValue; }

    /**
     * Open the popup below a trigger button.
     * @param anchorX  trigger's screen-space left edge
     * @param anchorY  trigger's screen-space bottom edge
     * @param width    preferred popup width (clamped to screen)
     */
    public DropdownPopup<T> openBelow(int anchorX, int anchorY, int width) {
        popW = Math.max(MIN_W, width);
        popX = Math.min(anchorX, x + this.width - popW - 2);
        popX = Math.max(x + 2, popX);
        popY = Math.min(anchorY, y + this.height - TOTAL_H - 2);
        popY = Math.max(y + 2, popY);
        return openInternal();
    }

    /** Open the popup at arbitrary screen-space coordinates. */
    public DropdownPopup<T> openAt(int screenX, int screenY, int width) {
        popW = Math.max(MIN_W, width);
        popX = Math.min(screenX, x + this.width - popW - 2);
        popX = Math.max(x + 2, popX);
        popY = Math.min(screenY, y + this.height - TOTAL_H - 2);
        popY = Math.max(y + 2, popY);
        return openInternal();
    }

    private DropdownPopup<T> openInternal() {
        open = true;
        searchField.value("");
        rebuildList("");
        mountChildren();
        searchField.setFocused(true);
        return this;
    }

    public void close()     { open = false; searchField.setFocused(false); }
    public boolean isOpen() { return open; }

    // ── Layout ────────────────────────────────────────────────

    @Override
    protected void onMounted() {
        mountChildren();
    }

    private void mountChildren() {
        if (popW <= 0) return;
        int innerY = popY + PAD;
        searchField.mount(popX + PAD, innerY, popW - PAD * 2, SEARCH_H);
        listBody.mount(popX + PAD, innerY + SEARCH_H + PAD, popW - PAD * 2, LIST_H);
    }

    // ── Rendering ─────────────────────────────────────────────

    @Override
    public void extract(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (!open) return;
        UITheme t = UITheme.current();
        RenderHelper.panel(g, popX, popY, popW, TOTAL_H, t.panel().bg(), t.panel().border());
        searchField.extractRenderState(g, mouseX, mouseY, delta);
        listBody.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override public boolean rendersAboveSiblings() { return open; }
    @Override public boolean blocksLowerInput()      { return open; }

    @Override
    protected boolean isFocusable() { return true; }

    @Override
    public @Nullable ComponentPath nextFocusPath(@NonNull FocusNavigationEvent event) { return null; }

    // ── Input ─────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!open) return false;
        double mx = event.x(), my = event.y();

        if (isOverPopup(mx, my)) {
            if (searchField.mouseClicked(event, doubleClick)) {
                searchField.setFocused(true);
                return true;
            }
            if (listBody.mouseClicked(event, doubleClick)) return true;
            return true; // absorb
        }
        close();
        return true; // absorb click-outside so it doesn't fire the trigger behind
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (open) { searchField.mouseReleased(event); listBody.mouseReleased(event); }
        return false;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dx, double dy) {
        return open && listBody.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        return open && listBody.mouseScrolled(mx, my, h, v);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (!open) return false;
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return searchField.keyPressed(event) || listBody.keyPressed(event);
    }

    @Override
    public boolean keyReleased(@NonNull KeyEvent event) {
        return open && searchField.keyReleased(event);
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        return open && searchField.charTyped(event);
    }

    @Override
    public boolean preeditUpdated(@Nullable PreeditEvent event) {
        return open && searchField.preeditUpdated(event);
    }

    // ── Internal ──────────────────────────────────────────────

    private boolean isOverPopup(double mx, double my) {
        return mx >= popX && mx < popX + popW && my >= popY && my < popY + TOTAL_H;
    }

    private void rebuildList(String query) {
        String lower = query.toLowerCase(Locale.ROOT);
        listBody = Containers.scrollable(Sizing.fill(), Sizing.fill()).gap(2).padding(Insets.of(2));
        allValues.stream()
                .filter(v -> labeler.apply(v).toLowerCase(Locale.ROOT).contains(lower))
                .forEach(v -> listBody.child(
                        WidgetComponents.button(labeler.apply(v), b -> select(v))
                                .sizing(Sizing.fill(), Sizing.fixed(16))));
        if (popW > 0) listBody.mount(popX + PAD, popY + PAD + SEARCH_H + PAD, popW - PAD * 2, LIST_H);
    }

    private void select(T v) {
        selectedValue = v;
        UISounds.playClick();
        close();
        if (onSelect != null) onSelect.accept(v);
    }
}
