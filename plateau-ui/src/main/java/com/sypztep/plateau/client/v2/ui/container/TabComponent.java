package com.sypztep.plateau.client.v2.ui.container;

import com.sypztep.plateau.client.v1.ui.core.RenderHelper;
import com.sypztep.plateau.client.v1.ui.core.UISounds;
import com.sypztep.plateau.client.v2.ui.core.*;
import com.sypztep.plateau.client.v2.ui.interaction.DragDrop;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Tabbed container with a scrollable header strip and animated content switching.
 * <p>
 * Unlike vanilla's {@code TabNavigationBar} (which shrinks every tab to an equal share of the
 * available width so the strip never overflows), tabs here keep their natural text width and
 * the header instead scrolls horizontally — via mouse wheel over the header, or automatically
 * into view on tab switch — once their combined width exceeds {@link #innerWidth()}.
 *
 * @see net.minecraft.client.gui.components.tabs.TabNavigationBar
 */
@Environment(EnvType.CLIENT)
public class TabComponent extends BaseContainerComponent<TabComponent> {

    private record Tab(Component title, BaseComponent<?> content) {}

    private final List<Tab> tabs = new ArrayList<>();

    private int activeIndex = 0;
    private int headerHeight = 22;
    private int headerGap = 3;
    private int contentGap = 6;
    private int tabPaddingX = 10;
    private float contentSlideSpeed = 0.35f;

    // Header scroll — same target/value exponential-lerp pattern as ScrollBehavior#update,
    // just for the header strip's horizontal axis instead of a container's vertical one.
    /** Animated horizontal scroll offset of the header strip, in pixels. */
    private double headerScrollX = 0;
    /** Target {@link #headerScrollX} lerps toward each frame. */
    private double headerScrollTarget = 0;

    // Per-tab hover animation — resized whenever tabs change.
    private float[] tabHoverProgress = new float[0];
    private int previousIndex = -1;
    private int slideDirection = 1;
    private float slideProgress = 1f;

    public TabComponent() {
        this.horizontalSizing = Sizing.fill();
        this.verticalSizing   = Sizing.fill();
    }

    public TabComponent tab(Component title, BaseComponent<?> content) {
        tabs.add(new Tab(title, content));
        tabHoverProgress = new float[tabs.size()]; // extend, new slots start at 0f
        return this;
    }

    public TabComponent tab(String title, BaseComponent<?> content) {
        return tab(Component.literal(title), content);
    }

    public TabComponent active(int index) {
        if (index >= 0 && index < tabs.size()) {
            if (index == activeIndex) return this;
            previousIndex = activeIndex;
            slideDirection = index > activeIndex ? 1 : -1;
            slideProgress = 0f;
            activeIndex = index;
            transferFocus(); // clear child focus on tab switch
            UISounds.playClick();
            mountContent(tabs.get(activeIndex).content());
            if (previousIndex >= 0) mountContent(tabs.get(previousIndex).content());
            ensureActiveTabVisible();
        }
        return this;
    }

    public int activeIndex()           { return activeIndex; }

    public @Nullable BaseComponent<?> activeContent() {
        if (tabs.isEmpty()) return null;
        return tabs.get(activeIndex).content();
    }

    public TabComponent headerHeight(int v)  { this.headerHeight = v; return this; }
    public TabComponent headerGap(int v)     { this.headerGap    = v; return this; }
    public TabComponent contentGap(int v)    { this.contentGap   = v; return this; }
    public TabComponent tabPaddingX(int v)   { this.tabPaddingX  = v; return this; }
    public TabComponent contentSlideAnimation(float speed) {
        this.contentSlideSpeed = Math.max(0f, speed);
        return this;
    }

    // ── ContainerEventHandler ─────────────────────────────────

    @Override
    public @NonNull List<BaseComponent<?>> children() {
        if (tabs.isEmpty()) return List.of();
        return List.of(tabs.get(activeIndex).content());
    }

    @Override
    public List<BaseComponent<?>> getChildren() {
        List<BaseComponent<?>> contents = new ArrayList<>(tabs.size());
        for (Tab tab : tabs) {
            contents.add(tab.content());
        }
        return contents;
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

        BaseComponent<?> content = activeContent();
        if (content != null && content.mouseClicked(event, doubleClick)) {
            if (content.shouldTakeFocusAfterInteraction()) setFocused(content);
            if (event.button() == 0) setDragging(true);
            return true;
        }

        setFocused(null);
        return false;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        setDragging(false); // was missing — BaseContainerComponent does not reset this automatically
        BaseComponent<?> content = activeContent();
        if (content != null) content.mouseReleased(event);
        return false;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dx, double dy) {
        BaseComponent<?> content = activeContent();
        return content != null && content.mouseDragged(event, dx, dy);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        BaseComponent<?> content = activeContent();
        if (content != null) content.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (isOverHeader(mouseX, mouseY)) {
            int maxScroll = Math.max(0, headerContentWidth() - innerWidth());
            double delta = hAmount != 0 ? hAmount : (vAmount < 0 ? 1 : -1);
            headerScrollTarget = Mth.clamp(headerScrollTarget + delta * 20, 0, maxScroll);
            return true;
        }

        BaseComponent<?> content = activeContent();
        return content != null
                && content.isMouseOver(mouseX, mouseY)
                && content.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
    }

    private boolean isOverHeader(double mouseX, double mouseY) {
        return mouseX >= innerX() && mouseX < innerX() + innerWidth()
                && mouseY >= innerY() && mouseY < innerY() + headerHeight;
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

        mountContent(tabs.get(activeIndex).content());
        if (previousIndex >= 0 && previousIndex < tabs.size()) {
            mountContent(tabs.get(previousIndex).content());
        }
    }

    private void mountContent(BaseComponent<?> content) {
        int contentX = innerX();
        int contentY = innerY() + headerHeight + contentGap;
        int contentW = innerWidth();
        int contentH = Math.max(0, innerHeight() - headerHeight - contentGap);

        content.mount(contentX, contentY, contentW, contentH);
    }

    @Override
    public int determineHorizontalContentSize(int space) {
        return padding.horizontal() + headerContentWidth();
    }

    /** Total natural width of every tab header button, including inter-tab gaps. */
    private int headerContentWidth() {
        int total = 0;
        for (int i = 0; i < tabs.size(); i++) {
            total += font.width(tabs.get(i).title()) + tabPaddingX * 2;
            if (i < tabs.size() - 1) total += headerGap;
        }
        return total;
    }

    private void clampHeaderScroll() {
        int maxScroll = Math.max(0, headerContentWidth() - innerWidth());
        headerScrollX = Mth.clamp(headerScrollX, 0, maxScroll);
        headerScrollTarget = Mth.clamp(headerScrollTarget, 0, maxScroll);
    }

    /** Lerps {@link #headerScrollX} toward {@link #headerScrollTarget}, mirroring {@code ScrollBehavior#update(float)}. */
    private void updateHeaderScrollAnimation(float delta) {
        if (Math.abs(headerScrollX - headerScrollTarget) > 0.1) {
            float lerpFactor = 1.0f - (float) Math.exp(-0.3f * delta);
            headerScrollX = Mth.lerp(lerpFactor, headerScrollX, headerScrollTarget);
        } else {
            headerScrollX = headerScrollTarget;
        }
    }

    /** Scrolls the header strip just far enough to bring the active tab's button into view. */
    private void ensureActiveTabVisible() {
        int tabX = 0;
        int tabW = 0;
        for (int i = 0; i <= activeIndex && i < tabs.size(); i++) {
            tabW = font.width(tabs.get(i).title()) + tabPaddingX * 2;
            if (i == activeIndex) break;
            tabX += tabW + headerGap;
        }

        if (tabX < headerScrollTarget) headerScrollTarget = tabX;
        else if (tabX + tabW > headerScrollTarget + innerWidth()) headerScrollTarget = tabX + tabW - innerWidth();
        clampHeaderScroll();
    }

    @Override
    public int determineVerticalContentSize(int space) {
        if (tabs.isEmpty()) return padding.vertical() + headerHeight;

        int contentW = Math.max(1, space - padding.horizontal());
        BaseComponent<?> content = tabs.get(activeIndex).content();

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

        BaseComponent<?> content = activeContent();
        if (content != null) extractContent(g, content, mouseX, mouseY, delta);
    }

    private void extractContent(GuiGraphicsExtractor graphics, BaseComponent<?> activeContent, int mouseX, int mouseY, float delta) {
        slideProgress = Mth.lerp(Mth.clamp(contentSlideSpeed * Math.max(0f, delta), 0f, 1f), slideProgress, 1f);
        if (slideProgress >= 0.995f) {
            slideProgress = 1f;
            previousIndex = -1;
        }

        int contentX = innerX();
        int contentY = innerY() + headerHeight + contentGap;
        int contentW = innerWidth();
        int contentH = Math.max(0, innerHeight() - headerHeight - contentGap);

        graphics.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);
        if (previousIndex >= 0 && previousIndex < tabs.size() && previousIndex != activeIndex) {
            int previousOffset = Math.round(Mth.lerp(easeOutCubic(slideProgress), 0f, -slideDirection * contentW));
            extractTranslatedContent(graphics, tabs.get(previousIndex).content(), previousOffset, hoverSuppressedMouse(), hoverSuppressedMouse(), delta);
        }

        int activeOffset = Math.round(Mth.lerp(easeOutCubic(slideProgress), slideDirection * contentW, 0f));
        extractTranslatedContent(graphics, activeContent, activeOffset, mouseX - activeOffset, mouseY, delta);
        graphics.disableScissor();
    }

    private void extractTranslatedContent(GuiGraphicsExtractor graphics, BaseComponent<?> content, int offsetX, int mouseX, int mouseY, float delta) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(offsetX, 0f);
        content.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.pose().popMatrix();
    }

    private static float easeOutCubic(float progress) {
        float inverse = 1f - Mth.clamp(progress, 0f, 1f);
        return 1f - inverse * inverse * inverse;
    }

    private void extractHeaders(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        clampHeaderScroll();
        updateHeaderScrollAnimation(delta);

        int headerX = innerX();
        int headerY = innerY();
        int headerW = innerWidth();

        g.enableScissor(headerX, headerY, headerX + headerW, headerY + headerHeight);
        int nextTabX = headerX - (int) Math.round(headerScrollX);

        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);

            int tabW = font.width(tab.title()) + tabPaddingX * 2;
            boolean selected = (i == activeIndex);
            boolean hovered  = mouseX >= nextTabX && mouseX < nextTabX + tabW
                    && mouseX >= headerX && mouseX < headerX + headerW
                    && mouseY >= headerY && mouseY < headerY + headerHeight;

            if (DragDrop.active() && hovered && !selected) {
                active(i);
                selected = true;
            }

            // Animate each tab's hover independently.
            tabHoverProgress[i] = stepAnimation(tabHoverProgress[i], hovered || selected, 0.5f, delta);

            float hover = selected ? Math.max(tabHoverProgress[i], 0.65f) : tabHoverProgress[i];
            float press = selected ? 0.18f : 0f;

            RenderHelper.squareButton(g, font, tab.title(), nextTabX, headerY, tabW, headerHeight,
                    true, hover, press, true);

            nextTabX += tabW + headerGap;
        }
        g.disableScissor();
    }

    private int tabAt(double mouseX, double mouseY) {
        if (mouseY < innerY() || mouseY >= innerY() + headerHeight) return -1;
        if (mouseX < innerX() || mouseX >= innerX() + innerWidth()) return -1;

        int nextTabX = innerX() - (int) Math.round(headerScrollX);
        for (int i = 0; i < tabs.size(); i++) {
            int tabW = font.width(tabs.get(i).title()) + tabPaddingX * 2;
            if (mouseX >= nextTabX && mouseX < nextTabX + tabW) return i;
            nextTabX += tabW + headerGap;
        }
        return -1;
    }

    // ── Fluent ────────────────────────────────────────────────
}
