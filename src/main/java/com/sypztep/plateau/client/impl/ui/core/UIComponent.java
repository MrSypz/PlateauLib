package com.sypztep.plateau.client.impl.ui.core;


import com.sypztep.plateau.client.impl.ui.theme.UITheme;
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
    @Nullable protected Component narrationMessage;

    protected final Minecraft minecraft;
    protected final Font font;

    // Shared hover animation — eliminates duplicated hoverAnimation field in every widget
    protected float hoverProgress = 0f;

    // Focus animation (smooth highlight when focused via keyboard/controller)
    protected float focusProgress = 0f;

    // Per-component sound config
    protected SoundConfig soundConfig = SoundConfig.silent();

    public UIComponent(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.minecraft = Minecraft.getInstance();
        this.font = minecraft.font;
    }

    // ═══════════════════════════════════════════
    // Rendering
    // ═══════════════════════════════════════════

    @Override
    public final void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        focusProgress = stepAnimation(focusProgress, focused, 0.1f);
        renderComponent(graphics, mouseX, mouseY, delta);
    }

    /**
     * Draw a focus ring around this component. Call at the end of renderComponent()
     * for components that want keyboard focus visibility.
     * UIButton already has its own — this is for panels, scroll areas, etc.
     */
    protected void renderFocusRing(GuiGraphics graphics) {
        if (focusProgress > 0.01f) {
            int alpha = (int)(255 * focusProgress);
            int color = UIColors.withAlpha(UITheme.current().textAccent(), alpha);
            RenderHelper.drawBorder(graphics, x - 1, y - 1, width + 2, height + 2, color);
        }
    }

    protected abstract void renderComponent(GuiGraphics graphics, int mouseX, int mouseY, float delta);

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
