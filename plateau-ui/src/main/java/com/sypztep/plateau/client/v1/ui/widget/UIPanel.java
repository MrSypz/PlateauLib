package com.sypztep.plateau.client.v1.ui.widget;

import com.sypztep.plateau.client.v1.ui.core.RenderHelper;
import com.sypztep.plateau.client.v1.ui.core.UIComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Visual container panel. Draws background, border, optional header.
 * Does NOT intercept input by default — add as renderable via addRenderable(),
 * not addWidget().
 */
public class UIPanel extends UIComponent {
    @Nullable protected Component title;
    protected boolean drawHeader;
    protected boolean drawBorder = true;
    private boolean interactable = false;

    public UIPanel(int x, int y, int width, int height, @Nullable Component title) {
        super(x, y, width, height);
        this.title = title;
        this.drawHeader = title != null;
        this.padding = 10;
        this.focusable = false;
    }

    public UIPanel(int x, int y, int width, int height) {
        this(x, y, width, height, null);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!interactable) return false;
        return super.isMouseOver(mouseX, mouseY);
    }

    @Override
    protected void renderComponent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (interactable) {
            hoverProgress = stepAnimation(hoverProgress, super.isMouseOver(mouseX, mouseY), 0.05f);
        }

        RenderHelper.drawPanelWithHover(graphics, x, y, width, height, hoverProgress, drawBorder);

        if (drawHeader && title != null) {
            RenderHelper.drawHeader(graphics, font, title, x, y, width, padding, hoverProgress);
        }

        renderContents(graphics, mouseX, mouseY, delta);
    }

    protected void renderContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {}

    @Override
    public int getContentY() {
        return y + (drawHeader && title != null ? font.lineHeight + padding * 3 : padding);
    }

    public UIPanel setTitle(@Nullable Component title) { this.title = title; this.drawHeader = title != null; return this; }
    public UIPanel setDrawHeader(boolean draw) { this.drawHeader = draw; return this; }
    public UIPanel setDrawBorder(boolean draw) { this.drawBorder = draw; return this; }
    public UIPanel setInteractable(boolean interactable) { this.interactable = interactable; return this; }
}
