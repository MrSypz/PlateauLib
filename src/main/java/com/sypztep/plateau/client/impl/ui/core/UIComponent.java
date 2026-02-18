package com.sypztep.plateau.client.impl.ui.core;

import com.sypztep.plateau.client.impl.ui.debug.DebugOverlay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class UIComponent implements GuiEventListener, Renderable, NarratableEntry {
    protected int x, y, width, height;
    protected int padding = 0;
    protected boolean visible = true;
    protected boolean focused = false;
    protected boolean focusable = true;
    @Nullable protected String debugLabel;
    @Nullable protected Component narrationMessage;

    protected final Minecraft minecraft;
    protected final Font font;

    protected UIComponent(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.minecraft = Minecraft.getInstance();
        this.font = minecraft.font;
        this.debugLabel = getClass().getSimpleName();
    }

    // ═══════════════════════════════════════════
    // Rendering
    // ═══════════════════════════════════════════

    @Override
    public final void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        renderComponent(graphics, mouseX, mouseY, delta);
        if (DebugOverlay.isEnabled()) {
            DebugOverlay.renderFor(graphics, this, mouseX, mouseY);
        }
    }

    protected abstract void renderComponent(GuiGraphics graphics, int mouseX, int mouseY, float delta);

    // ═══════════════════════════════════════════
    // Animation helper
    // ═══════════════════════════════════════════

    protected static float stepAnimation(float current, boolean active, float speed) {
        if (active) return Math.min(1f, current + speed);
        else return Math.max(0f, current - speed);
    }

    // ═══════════════════════════════════════════
    // GuiEventListener
    // ═══════════════════════════════════════════

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) { return false; }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) { return false; }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) { return false; }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) { return false; }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return visible && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public void setFocused(boolean focused) { this.focused = focused; }

    @Override
    public boolean isFocused() { return focused; }

    /**
     * Screen uses this to know the component's position for arrow key navigation.
     * It compares rectangles to decide which component is "to the right of" or "below" the current one.
     */
    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(x, y, width, height);
    }

    /**
     * Returning a non-null path here tells Screen "yes, I can receive focus".
     * Without this, arrow keys and tab key skip this component entirely.
     */
    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        if (!visible || !focusable) return null;
        // If already focused, return null so focus moves to the next component
        if (focused) return null;
        return ComponentPath.leaf(this);
    }

    // ═══════════════════════════════════════════
    // NarratableEntry — arrow key + tab key support
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

    /**
     * Helper — can't check mouse position outside render, but narrationPriority
     * is called by Screen during narration updates. Use a cached value.
     */
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
    public UIComponent setDebugLabel(String label) { this.debugLabel = label; return this; }
    @Nullable public String getDebugLabel() { return debugLabel; }
    public UIComponent setNarrationMessage(Component message) { this.narrationMessage = message; return this; }
}