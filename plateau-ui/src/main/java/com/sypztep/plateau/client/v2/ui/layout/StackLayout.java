package com.sypztep.plateau.client.v2.ui.layout;

import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.BaseContainerComponent;
import com.sypztep.plateau.client.v2.ui.core.Insets;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.core.Surface;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.NonNull;

/**
 * Overlay container.
 *
 * Every child is mounted to the same inner area.
 * Later children render above earlier children.
 *
 * Use for screen roots that need modal/dialog overlays.
 */
@Environment(EnvType.CLIENT)
public class StackLayout extends BaseContainerComponent {

    public StackLayout(Sizing horizontal, Sizing vertical) {
        this.horizontalSizing = horizontal;
        this.verticalSizing = vertical;
    }

    @Override
    protected void onMounted() {
        layout();
    }

    private void layout() {
        int ix = innerX();
        int iy = innerY();
        int iw = innerWidth();
        int ih = innerHeight();

        for (BaseComponent child : children) {
            if (!child.isVisible()) continue;

            int childX = ix + child.margins().left();
            int childY = iy + child.margins().top();
            int childW = Math.max(0, iw - child.margins().horizontal());
            int childH = Math.max(0, ih - child.margins().vertical());

            child.mount(childX, childY, childW, childH);
        }
    }

    @Override
    public void extract(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        for (BaseComponent child : children) {
            if (child.isVisible()) {
                child.extractRenderState(g, mouseX, mouseY, delta);
            }
        }
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!isMouseOver(event.x(), event.y())) return false;

        // Top-most child first.
        for (int i = children.size() - 1; i >= 0; i--) {
            BaseComponent child = children.get(i);
            if (!child.isVisible()) continue;
            if (!child.isMouseOver(event.x(), event.y())) continue;

            if (child.mouseClicked(event, doubleClick)) {
                if (child != getFocused() && child.shouldTakeFocusAfterInteraction()) {
                    setFocused(child);
                }

                if (event.button() == 0) {
                    setDragging(true);
                }

                return true;
            }
        }

        setFocused(null);
        return false;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        setDragging(false);

        GuiEventListener focused = getFocused();
        if (focused != null) {
            return focused.mouseReleased(event);
        }

        return false;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        GuiEventListener focused = getFocused();
        return focused != null && focused.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (!isMouseOver(mouseX, mouseY)) return false;

        // Top-most child first.
        for (int i = children.size() - 1; i >= 0; i--) {
            BaseComponent child = children.get(i);
            if (!child.isVisible()) continue;
            if (!child.isMouseOver(mouseX, mouseY)) continue;

            if (child.mouseScrolled(mouseX, mouseY, hAmount, vAmount)) {
                return true;
            }

            // Stop at first child under mouse so lower layers don't receive scroll through it.
            return false;
        }

        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        for (int i = children.size() - 1; i >= 0; i--) {
            BaseComponent child = children.get(i);
            if (child.isVisible()) {
                child.mouseMoved(mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        return getFocused() != null && getFocused().keyPressed(event);
    }

    @Override public StackLayout child(BaseComponent child)                             { super.child(child);         return this; }
    @Override public StackLayout children(BaseComponent... components)                  { super.children(components); return this; }
    @Override public StackLayout children(Iterable<? extends BaseComponent> components) { super.children(components); return this; }

    @Override public StackLayout padding(Insets padding)   { super.padding(padding); return this; }
    @Override public StackLayout margins(Insets margins)   { super.margins(margins); return this; }
    @Override public StackLayout surface(Surface surface)  { super.surface(surface); return this; }
    @Override public StackLayout id(String id)             { super.id(id); return this; }
    @Override public StackLayout visible(boolean visible)  { super.visible(visible); return this; }
    @Override public StackLayout sizing(Sizing h, Sizing v){ super.sizing(h, v); return this; }
    @Override public StackLayout sizing(Sizing both)       { super.sizing(both); return this; }
}