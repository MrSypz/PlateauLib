package com.sypztep.plateau.client.ui.screen;

import com.sypztep.plateau.client.ui.core.UIComponent;
import com.sypztep.plateau.client.ui.theme.UITheme;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Base screen that manages both Screen widgets AND UIComponents.
 */
@Environment(EnvType.CLIENT)
public abstract class PlateauScreen extends Screen {
    protected final List<UIComponent> components = new ArrayList<>();
    protected TabManager tabManager;

    protected PlateauScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        components.clear();
        initComponents();

        if (tabManager != null) {
            tabManager.init();
        }
    }

    protected abstract void initComponents();

    // ── UIComponent management ──

    public <T extends UIComponent> T addComponent(T component) {
        components.add(component);
        return component;
    }

    public void removeComponent(UIComponent component) {
        components.remove(component);
    }

    // ── Tab widget management (delegates to Screen's widget system) ──

    public <T extends GuiEventListener & Renderable & NarratableEntry> T addTabWidget(T widget) {
        return addRenderableWidget(widget);
    }

    public <T extends Renderable> T addTabRenderable(T renderable) {
        return addRenderableOnly(renderable);
    }

    public void removeTabWidget(GuiEventListener widget) {
        removeWidget(widget);
    }

    // ── Content start Y (for tabs to know where content area begins) ──

    public int getContentStartY() {
        return 30; // Below title + nav bar area
    }

    // ── Rendering ──

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fillGradient(0, 0, width, height, UITheme.SCREEN_BACKGROUND, UITheme.SCREEN_BACKGROUND);
        super.render(graphics, mouseX, mouseY, delta);

        // Render UIComponents
        for (UIComponent component : components) {
            component.render(graphics, mouseX, mouseY, delta);
        }

        // Title
        graphics.drawCenteredString(font, title, width / 2, 10, UITheme.TEXT_PRIMARY);

        if (tabManager != null) {
            tabManager.renderOverlay(graphics, mouseX, mouseY, delta);
        }
    }

    // ── Input routing: UIComponents first, then Screen's widget system ──

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        for (UIComponent c : components) {
            if (c.isMouseOver(mouseX, mouseY) && c.mouseScrolled(mouseX, mouseY, hAmount, vAmount)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        for (UIComponent c : components) {
            if (c.mouseClicked(event, bl)) return true;
        }
        return super.mouseClicked(event, bl);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        for (UIComponent c : components) {
            if (c.mouseDragged(event, dragX, dragY)) return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (UIComponent c : components) {
            if (c.mouseReleased(event)) return true;
        }
        return super.mouseReleased(event);
    }
}