package com.sypztep.plateau.client.v2.ui.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

/**
 * Base for all v2 UI components. Unlike v1, components do not take x/y/w/h in their constructors —
 * the parent layout assigns those via {@link #mount}. Declare what size you want with
 * {@link Sizing} and let the layout engine do the math.
 */
@Environment(EnvType.CLIENT)
public abstract class BaseComponent<GenericComponent extends BaseComponent<GenericComponent>> implements GuiEventListener, Renderable, NarratableEntry {

    protected int x, y, width, height;
    protected Sizing horizontalSizing = Sizing.content();
    protected Sizing verticalSizing   = Sizing.content();
    protected Insets padding  = Insets.none();
    protected Insets margins  = Insets.none();
    protected Surface surface = Surface.NONE;
    protected boolean visible  = true;
    protected boolean focused  = false;
    protected @Nullable String id;

    protected final Minecraft minecraft;
    protected final Font font;

    protected BaseComponent() {
        this.minecraft = Minecraft.getInstance();
        this.font      = minecraft.font;
    }

    // ── Layout ───────────────────────────────────────────────

    /** Called by the parent layout once it has computed this component's position and size. */
    public final void mount(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width  = Math.max(0, width);
        this.height = Math.max(0, height);
        onMounted();
    }

    /** Override to react after mount() has been called (e.g. lay out children). */
    protected void onMounted() {}

    public void setX(int x) {
        mount(x, y, width, height);
    }

    public void setY(int y) {
        mount(x, y, width, height);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Return the preferred width when {@link Sizing#content()} is used horizontally.
     * @param space the available width offered by the parent
     */
    public int determineHorizontalContentSize(int space) { return 0; }

    /**
     * Return the preferred height when {@link Sizing#content()} is used vertically.
     * @param space the available height offered by the parent
     */
    public int determineVerticalContentSize(int space) { return 0; }

    // ── Rendering ────────────────────────────────────────────

    @Override
    public final void extractRenderState(@NonNull GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        surface.extract(g, x, y, width, height);
        extract(g, mouseX, mouseY, delta);
    }

    /** Override to render this component's content. Surface is already drawn before this is called. */
    public abstract void extract(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta);

    // ── Animation helpers ─────────────────────────────────────

    protected static float stepAnimation(float current, boolean active, float speed, float delta) {
        float amount = speed * delta;

        if (active) return Math.min(1f, current + amount);
        else        return Math.max(0f, current - amount);
    }

    // ── GuiEventListener ─────────────────────────────────────

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) { return false; }
    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) { return false; }
    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) { return false; }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) { return false; }
    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) { return false; }

    /**
     * When true, parent overlays must route input to this component before lower siblings
     * and stop propagation even if this component does not otherwise handle the event.
     */
    public boolean blocksLowerInput() { return false; }

    public boolean shouldTakeFocusAfterInteraction() { return isFocusable(); }

    public int renderClipTopOutset() { return 0; }
    public int renderClipRightOutset() { return 0; }
    public int renderClipBottomOutset() { return 0; }
    public int renderClipLeftOutset() { return 0; }
    public boolean rendersAboveSiblings() { return false; }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return hitTest(mouseX, mouseY);
    }

    @Override
    public void setFocused(boolean focused) { this.focused = focused; }
    @Override
    public boolean isFocused() { return focused; }

    @Override
    public @NonNull ScreenRectangle getRectangle() {
        return bounds();
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        if (!visible || !isFocusable()) return null;
        if (focused) return null;
        return ComponentPath.leaf(this);
    }

    protected boolean isFocusable() { return false; }

    // ── Hit Testing ───────────────────────────────────

    /**
     * Bounds check using content-space coordinates (already scroll-adjusted by the parent).
     * Matches the same bounds used by layout, so hit-test and rendered position always agree.
     */
    public boolean hitTest(double x, double y) {
        return visible && bounds().containsPoint(Mth.floor(x), Mth.floor(y));
    }

    // ── NarratableEntry ───────────────────────────────────────

    @Override
    public @NonNull NarrationPriority narrationPriority() { return NarrationPriority.NONE; }
    @Override
    public void updateNarration(@NonNull NarrationElementOutput output) {}

    // ── Fluent API ────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    protected final GenericComponent self() {
        return (GenericComponent) this;
    }

    public GenericComponent sizing(Sizing horizontal, Sizing vertical) {
        this.horizontalSizing = horizontal;
        this.verticalSizing   = vertical;
        return self();
    }

    public GenericComponent sizing(Sizing both) {
        return sizing(both, both);
    }

    public GenericComponent padding(Insets padding) {
        this.padding = padding;
        return self();
    }

    public GenericComponent padding(int all) {
        return padding(Insets.of(all));
    }

    public GenericComponent margins(Insets margins) {
        this.margins = margins;
        return self();
    }

    public GenericComponent margins(int all) {
        return margins(Insets.of(all));
    }

    public GenericComponent margin(Insets margins) {
        return margins(margins);
    }

    public GenericComponent margin(int all) {
        return margins(all);
    }

    public GenericComponent surface(Surface surface) {
        this.surface = surface;
        return self();
    }

    public GenericComponent id(String id) {
        this.id = id;
        return self();
    }

    public GenericComponent visible(boolean visible) {
        this.visible = visible;
        return self();
    }

    // ── Geometry accessors ────────────────────────────────────

    public int x()       { return x; }
    public int y()       { return y; }
    public int width()   { return width; }
    public int height()  { return height; }
    public ScreenRectangle bounds() { return new ScreenRectangle(x, y, width, height); }
    public ScreenRectangle innerBounds() { return new ScreenRectangle(innerX(), innerY(), innerWidth(), innerHeight()); }
    public Sizing horizontalSizing() { return horizontalSizing; }
    public Sizing verticalSizing()   { return verticalSizing; }
    public Insets padding()  { return padding; }
    public Insets margins()  { return margins; }
    public @Nullable String id() { return id; }
    public boolean isVisible() { return visible; }

    public int innerX() { return x + padding.left(); }
    public int innerY() { return y + padding.top(); }
    public int innerWidth()  { return Math.max(0, width  - padding.horizontal()); }
    public int innerHeight() { return Math.max(0, height - padding.vertical()); }
}
