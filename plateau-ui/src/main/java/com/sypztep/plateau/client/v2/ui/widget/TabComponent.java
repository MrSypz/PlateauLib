package com.sypztep.plateau.client.v2.ui.widget;

import com.sypztep.plateau.client.v1.ui.core.RenderHelper;
import com.sypztep.plateau.client.v1.ui.core.UISounds;
import com.sypztep.plateau.client.v2.ui.core.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class TabComponent extends BaseContainerComponent {

    private record Tab(Component title, BaseComponent content) {}

    private final List<Tab> tabs = new ArrayList<>();

    private int activeIndex = 0;
    private int headerHeight = 22;
    private int headerGap = 3;
    private int contentGap = 6;
    private int tabPaddingX = 10;

    // Per-tab hover animation — resized whenever tabs change.
    private float[] tabHoverProgress = new float[0];

    public TabComponent() {
        this.horizontalSizing = Sizing.fill();
        this.verticalSizing   = Sizing.fill();
    }

    public TabComponent tab(Component title, BaseComponent content) {
        tabs.add(new Tab(title, content));
        tabHoverProgress = new float[tabs.size()]; // extend, new slots start at 0f
        return this;
    }

    public TabComponent tab(String title, BaseComponent content) {
        return tab(Component.literal(title), content);
    }

    public TabComponent active(int index) {
        if (index >= 0 && index < tabs.size()) {
            activeIndex = index;
            transferFocus(); // clear child focus on tab switch
            UISounds.playClick();
            mountActiveContent();
        }
        return this;
    }

    public int activeIndex()           { return activeIndex; }

    public BaseComponent activeContent() {
        if (tabs.isEmpty()) return null;
        return tabs.get(activeIndex).content();
    }

    public TabComponent headerHeight(int v)  { this.headerHeight = v; return this; }
    public TabComponent headerGap(int v)     { this.headerGap    = v; return this; }
    public TabComponent contentGap(int v)    { this.contentGap   = v; return this; }
    public TabComponent tabPaddingX(int v)   { this.tabPaddingX  = v; return this; }

    // ── ContainerEventHandler ─────────────────────────────────

    @Override
    public @NonNull List<? extends GuiEventListener> children() {
        if (tabs.isEmpty()) return List.of();
        return List.of(tabs.get(activeIndex).content());
    }

    // ── Input ─────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!isMouseOver(event.x(), event.y())) return false;

        int tab = tabAt(event.x(), event.y());
        if (tab >= 0) {
            if (tab != activeIndex) active(tab);
            setFocused(true);
            return true;
        }

        BaseComponent content = activeContent();
        if (content != null && content.mouseClicked(event, doubleClick)) {
            if (content.shouldTakeFocusAfterInteraction()) setFocused(content);
            if (event.button() == 0) setDragging(true);
            return true;
        }

        setFocused(null);
        return false;
    }

    @Override
    public boolean onPointerClicked(MouseButtonEvent event, boolean doubleClick, double x, double y) {
        if (!hitTest(x, y)) return false;

        int tab = tabAt(x, y);
        if (tab >= 0) {
            if (tab != activeIndex) active(tab);
            setFocused(true);
            return true;
        }

        BaseComponent content = activeContent();
        if (content != null && content.hitTest(x, y)) {
            if (content.onPointerClicked(event, doubleClick, x, y)) {
                if (content.shouldTakeFocusAfterInteraction()) setFocused(content);
                if (event.button() == 0) setDragging(true);
                return true;
            }
        }

        setFocused(null);
        return false;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        setDragging(false); // was missing — BaseContainerComponent does not reset this automatically
        BaseComponent content = activeContent();
        if (content != null) content.mouseReleased(event);
        return false;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dx, double dy) {
        BaseComponent content = activeContent();
        return content != null && content.mouseDragged(event, dx, dy);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        BaseComponent content = activeContent();
        if (content != null) content.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        BaseComponent content = activeContent();
        return content != null
                && content.isMouseOver(mouseX, mouseY)
                && content.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (getFocused() != null && getFocused().keyPressed(event)) return true;

        int key = event.key();
        if (key == GLFW.GLFW_KEY_LEFT)  { active(Math.max(0, activeIndex - 1));               return true; }
        if (key == GLFW.GLFW_KEY_RIGHT) { active(Math.min(tabs.size() - 1, activeIndex + 1)); return true; }

        return false;
    }

    // ── Layout ────────────────────────────────────────────────

    @Override
    protected void onMounted() {
        mountActiveContent();
    }

    private void mountActiveContent() {
        if (tabs.isEmpty()) return;

        BaseComponent content = tabs.get(activeIndex).content();

        int contentX = innerX();
        int contentY = innerY() + headerHeight + contentGap;
        int contentW = innerWidth();
        int contentH = Math.max(0, innerHeight() - headerHeight - contentGap);

        content.mount(contentX, contentY, contentW, contentH);
    }

    @Override
    public int determineHorizontalContentSize(int space) {
        int total = padding.horizontal();
        for (int i = 0; i < tabs.size(); i++) {
            total += font.width(tabs.get(i).title()) + tabPaddingX * 2;
            if (i < tabs.size() - 1) total += headerGap;
        }
        return total;
    }

    @Override
    public int determineVerticalContentSize(int space) {
        if (tabs.isEmpty()) return padding.vertical() + headerHeight;

        int contentW = Math.max(1, space - padding.horizontal());
        BaseComponent content = tabs.get(activeIndex).content();

        int contentH = switch (content.verticalSizing()) {
            case Sizing.Fixed   f       -> f.value();
            case Sizing.Fill    ignored -> content.determineVerticalContentSize(contentW);
            case Sizing.Content ignored -> content.determineVerticalContentSize(contentW);
        };

        return padding.vertical() + headerHeight + contentGap + contentH;
    }

    // ── Rendering ─────────────────────────────────────────────

    @Override
    public void extract(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        extractHeaders(g, mouseX, mouseY, delta);

        BaseComponent content = activeContent();
        if (content != null) content.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void extractHeaders(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int curX = innerX();
        int tabY = innerY();

        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);

            int tabW = font.width(tab.title()) + tabPaddingX * 2;
            boolean selected = (i == activeIndex);
            boolean hovered  = mouseX >= curX && mouseX < curX + tabW
                    && mouseY >= tabY && mouseY < tabY + headerHeight;

            // Animate each tab's hover independently.
            tabHoverProgress[i] = stepAnimation(tabHoverProgress[i], hovered || selected, 0.5f, delta);

            float hover = selected ? Math.max(tabHoverProgress[i], 0.65f) : tabHoverProgress[i];
            float press = selected ? 0.18f : 0f;

            RenderHelper.squareButton(g, font, tab.title(), curX, tabY, tabW, headerHeight,
                    true, hover, press, true);

            curX += tabW + headerGap;
        }
    }

    private int tabAt(double mouseX, double mouseY) {
        if (mouseY < innerY() || mouseY >= innerY() + headerHeight) return -1;

        int curX = innerX();
        for (int i = 0; i < tabs.size(); i++) {
            int tabW = font.width(tabs.get(i).title()) + tabPaddingX * 2;
            if (mouseX >= curX && mouseX < curX + tabW) return i;
            curX += tabW + headerGap;
        }
        return -1;
    }

    // ── Fluent ────────────────────────────────────────────────

    @Override public TabComponent padding(Insets padding)    { super.padding(padding); return this; }
    @Override public TabComponent margins(Insets margins)    { super.margins(margins); return this; }
    @Override public TabComponent surface(Surface surface)   { super.surface(surface); return this; }
    @Override public TabComponent id(String id)              { super.id(id);           return this; }
    @Override public TabComponent visible(boolean visible)   { super.visible(visible); return this; }
    @Override public TabComponent sizing(Sizing h, Sizing v) { super.sizing(h, v);     return this; }
    @Override public TabComponent sizing(Sizing both)        { super.sizing(both);     return this; }
}