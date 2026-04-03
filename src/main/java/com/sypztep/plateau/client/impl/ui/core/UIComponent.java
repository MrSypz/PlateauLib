package com.sypztep.plateau.client.impl.ui.core;


import com.sypztep.plateau.client.impl.ui.behavior.ScrollBehavior;
import com.sypztep.plateau.client.impl.ui.theme.UITheme;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

@Environment(EnvType.CLIENT)
public abstract class UIComponent implements GuiEventListener, Renderable, NarratableEntry {
    protected int x, y, width, height;
    protected int padding = 0;
    protected boolean visible = true;
    protected boolean focused = false;
    protected boolean focusable = true;
    @Nullable protected Component narrationMessage;

    protected final Minecraft minecraft;
    protected final Font font;

    // Shared hover animation — eliminates duplicated hoverAnimation field in every widget
    protected float hoverProgress = 0f;

    // Focus animation (smooth highlight when focused via keyboard/controller)
    protected float focusProgress = 0f;

    // Per-component sound config
    protected SoundConfig soundConfig = SoundConfig.silent();

    // Optional scroll behavior — when set, keyboard/mouse scroll is handled automatically
    @Nullable protected ScrollBehavior scroll;

    public UIComponent(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.minecraft = Minecraft.getInstance();
        this.font = minecraft.font;
    }

    // ═══════════════════════════════════════════
    // Scroll — engine-level support
    // ═══════════════════════════════════════════

    /**
     * Enable scrolling on this component. Once set, the engine handles:
     * <ul>
     *   <li>Mouse wheel scrolling</li>
     *   <li>Scrollbar click/drag</li>
     *   <li>Keyboard: Arrow up/down, Page up/down, Home/End (when focused)</li>
     *   <li>Scrollbar rendering</li>
     * </ul>
     * You only need to call {@code scroll.setBounds()}, {@code scroll.setContentHeight()},
     * {@code scroll.update()}, {@code scroll.enableScissor()}/{@code disableScissor()} in your render.
     * <p>
     * Override {@link #onKeyScroll(int)} for custom key behavior (e.g., item-based navigation).
     */
    public UIComponent enableScrolling() {
        this.scroll = new ScrollBehavior();
        return this;
    }

    public UIComponent enableScrolling(ScrollBehavior scroll) {
        this.scroll = scroll;
        return this;
    }

    @Nullable
    public ScrollBehavior getScroll() { return scroll; }

    /**
     * Override to customize keyboard scroll step (default 20px).
     */
    protected int getScrollStep() { return 20; }

    /**
     * Override for custom key handling when scrolling is enabled.
     * Return true to consume the key event.
     * Called BEFORE the default scroll key handling.
     */
    protected boolean onKeyScroll(int key) { return false; }

    // ═══════════════════════════════════════════
    // Rendering
    // ═══════════════════════════════════════════

    @Override
    public final void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        focusProgress = stepAnimation(focusProgress, focused, 0.1f);
        renderComponent(graphics, mouseX, mouseY, delta);
    }
    /**
     * Draw a focus ring around this component. Call at the end of renderComponent()
     * for components that want keyboard focus visibility.
     * UIButton already has its own — this is for panels, scroll areas, etc.
     */
    protected void renderFocusRing(GuiGraphicsExtractor graphics) {
        if (focusProgress > 0.01f) {
            int alpha = (int)(255 * focusProgress);
            int color = UIColors.withAlpha(UITheme.current().textAccent(), alpha);
            RenderHelper.drawBorder(graphics, x - 1, y - 1, width + 2, height + 2, color);
        }
    }

    protected abstract void renderComponent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta);

    // ═══════════════════════════════════════════
    // Animation helpers
    // ═══════════════════════════════════════════

    protected static float stepAnimation(float current, boolean active, float speed) {
        if (active) return Math.min(1f, current + speed);
        else return Math.max(0f, current - speed);
    }

    /**
     * Update hover animation. Call at the top of renderComponent() to auto-track hover state.
     */
    protected void updateHover(int mouseX, int mouseY) {
        boolean hovered = isMouseOver(mouseX, mouseY);
        setHoveredCache(hovered);
        hoverProgress = stepAnimation(hoverProgress, hovered || focused, 0.05f);
    }

    public float getHoverProgress() { return hoverProgress; }

    // ═══════════════════════════════════════════
    // GuiEventListener — auto-delegates to scroll
    // ═══════════════════════════════════════════

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        return scroll != null && scroll.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        return scroll != null && scroll.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        return scroll != null && scroll.mouseDragged(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        return scroll != null && scroll.mouseScrolled(mouseX, mouseY, vAmount);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) {
        if (!focused || scroll == null) return false;

        // Let subclass handle first
        if (onKeyScroll(keyEvent.key())) return true;

        int key = keyEvent.key();
        if (key == 265) { scroll.scrollBy(-getScrollStep()); return true; } // UP
        if (key == 264) { scroll.scrollBy(getScrollStep()); return true; }  // DOWN
        if (key == 266) { scroll.scrollBy(-(height - 20)); return true; }   // PAGE_UP
        if (key == 267) { scroll.scrollBy(height - 20); return true; } // PAGE_DOWN
        if (key == 268) { scroll.scrollTo(0); return true; }          // HOME
        if (key == 269) { scroll.scrollToEnd(); return true; }              // END

        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return visible && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public void setFocused(boolean focused) {
        if (focused && !this.focused && focusable) {
            UISounds.playFocus();
        }
        this.focused = focused;
    }

    @Override
    public boolean isFocused() { return focused; }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(x, y, width, height);
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        if (!visible || !focusable) return null;
        if (focused) return null;
        return ComponentPath.leaf(this);
    }

    // ═══════════════════════════════════════════
    // NarratableEntry
    // ═══════════════════════════════════════════

    @Override
    public NarrationPriority narrationPriority() {
        if (focused) return NarrationPriority.FOCUSED;
        if (isHoveredNow()) return NarrationPriority.HOVERED;
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
        if (narrationMessage != null) {
            output.add(NarratedElementType.TITLE, narrationMessage);
        }
    }

    private boolean hoveredCache = false;
    protected void setHoveredCache(boolean hovered) { this.hoveredCache = hovered; }
    private boolean isHoveredNow() { return hoveredCache; }

    // ═══════════════════════════════════════════
    // Geometry
    // ═══════════════════════════════════════════

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getPadding() { return padding; }
    public int getContentX() { return x + padding; }
    public int getContentY() { return y + padding; }
    public int getContentWidth() { return width - padding * 2; }
    public int getContentHeight() { return height - padding * 2; }

    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public void setSize(int width, int height) { this.width = width; this.height = height; }
    public void setBounds(int x, int y, int width, int height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }

    public UIComponent setPadding(int padding) { this.padding = padding; return this; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public UIComponent setFocusable(boolean focusable) { this.focusable = focusable; return this; }
    public UIComponent setNarrationMessage(Component message) { this.narrationMessage = message; return this; }
    public UIComponent setSoundConfig(SoundConfig config) { this.soundConfig = config; return this; }
    public SoundConfig getSoundConfig() { return soundConfig; }
}
