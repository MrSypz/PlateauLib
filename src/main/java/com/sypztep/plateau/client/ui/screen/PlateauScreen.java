package com.sypztep.plateau.client.ui.screen;

import com.sypztep.plateau.client.ui.debug.DebugOverlay;
import com.sypztep.plateau.client.ui.theme.UITheme;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public abstract class PlateauScreen extends Screen {
    protected TabManager tabManager;

    protected PlateauScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        if (tabManager != null) {
            tabManager.clearTracking();
        }

        initComponents();

        if (tabManager != null) {
            tabManager.init();
        }
    }

    protected abstract void initComponents();

    // Bridge for Tab
    public <T extends GuiEventListener & Renderable & NarratableEntry> T addTabWidget(T widget) {
        return addRenderableWidget(widget);
    }

    public <T extends Renderable> T addTabRenderable(T renderable) {
        return addRenderableOnly(renderable);
    }

    public void removeTabWidget(GuiEventListener widget) {
        removeWidget(widget);
    }

    /**
     * Content start Y — below NavBar if TabManager exists.
     */
    public int getContentStartY() {
        if (tabManager != null) {
            return tabManager.getNavBarHeight() + 10;
        }
        return 25;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, width, height, UITheme.SCREEN_BACKGROUND, UITheme.SCREEN_BACKGROUND);

        DebugOverlay.beginFrame(width, height);

        // Screen renders all widgets (NavBar + tab content)
        super.render(graphics, mouseX, mouseY, delta);

        // Tab overlay (tooltips, etc.)
        if (tabManager != null) {
            tabManager.renderOverlay(graphics, mouseX, mouseY, delta);
        }

        DebugOverlay.renderHUD(graphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.key() == 292) { // F3
            DebugOverlay.toggle();
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (DebugOverlay.isEnabled() && DebugOverlay.handleClick(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseClicked(event, bl);
    }
}