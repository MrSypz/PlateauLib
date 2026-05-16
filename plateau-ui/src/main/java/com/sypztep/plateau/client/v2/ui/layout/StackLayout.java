package com.sypztep.plateau.client.v2.ui.layout;

import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.BaseContainerComponent;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
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
public class StackLayout extends BaseContainerComponent<StackLayout> {

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

        for (BaseComponent<?> child : children) {
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
        int blockingIndex = blockingChildIndex(mouseX, mouseY);
        for (int index = 0; index < children.size(); index++) {
            BaseComponent<?> child = children.get(index);
            if (child.isVisible()) {
                int childMouseX = index < blockingIndex ? hoverSuppressedMouse() : mouseX;
                int childMouseY = index < blockingIndex ? hoverSuppressedMouse() : mouseY;
                child.extractRenderState(g, childMouseX, childMouseY, delta);
            }
        }
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!isMouseOver(event.x(), event.y())) return false;

        // Top-most child first. Blocking children absorb even when their internals don't.
        for (int i = children.size() - 1; i >= 0; i--) {
            BaseComponent<?> child = children.get(i);
            if (!child.isVisible()) continue;
            boolean blocking = child.blocksLowerInput();
            if (!blocking && !child.isMouseOver(event.x(), event.y())) continue;

            if (child.mouseClicked(event, doubleClick)) {
                focusAfterInteraction(child, event.button());
                return true;
            }

            if (blocking) return true;
        }

        setFocused(null);
        return false;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        boolean wasDragging = isDragging();
        if (event.button() == 0) setDragging(false);

        for (int i = children.size() - 1; i >= 0; i--) {
            BaseComponent<?> child = children.get(i);
            if (!child.isVisible() || !child.blocksLowerInput()) continue;
            child.mouseReleased(event);
            return true;
        }

        GuiEventListener focused = getFocused();
        if (focused != null && (wasDragging || focused.isMouseOver(event.x(), event.y()))) {
            if (focused.mouseReleased(event)) return true;
        }

        for (int i = children.size() - 1; i >= 0; i--) {
            BaseComponent<?> child = children.get(i);
            if (!child.isVisible()) continue;
            boolean blocking = child.blocksLowerInput();
            if (!blocking && !child.isMouseOver(event.x(), event.y())) continue;

            if (child.mouseReleased(event)) return true;
            if (blocking) return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        for (int i = children.size() - 1; i >= 0; i--) {
            BaseComponent<?> child = children.get(i);
            if (!child.isVisible() || !child.blocksLowerInput()) continue;
            child.mouseDragged(event, dragX, dragY);
            return true;
        }

        GuiEventListener focused = getFocused();
        return focused != null && focused.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (!isMouseOver(mouseX, mouseY)) return false;

        // Top-most child first.
        // Blocking children absorb scroll even when they don't handle it.
        // Non-blocking children that don't handle scroll are transparent — the event continues
        // to the next child. This allows fill×fill overlays (ContextMenu, HoverCard) to sit
        // on top without swallowing scroll when they are inactive.
        for (int i = children.size() - 1; i >= 0; i--) {
            BaseComponent<?> child = children.get(i);
            if (!child.isVisible()) continue;
            boolean blocking = child.blocksLowerInput();
            if (!blocking && !child.isMouseOver(mouseX, mouseY)) continue;

            if (child.mouseScrolled(mouseX, mouseY, hAmount, vAmount)) return true;
            if (blocking) return false; // blocking but didn't handle = still absorbs
            // non-blocking and didn't handle = fall through to the child below
        }

        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        int blockingIndex = blockingChildIndex(mouseX, mouseY);
        for (int i = children.size() - 1; i >= 0; i--) {
            if (i < blockingIndex) continue;
            BaseComponent<?> child = children.get(i);
            if (child.isVisible()) {
                child.mouseMoved(mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        for (int i = children.size() - 1; i >= 0; i--) {
            BaseComponent<?> child = children.get(i);
            if (!child.isVisible() || !child.blocksLowerInput()) continue;
            child.keyPressed(event);
            return true;
        }

        return getFocused() != null && getFocused().keyPressed(event);
    }

    private int blockingChildIndex(double mouseX, double mouseY) {
        for (int index = children.size() - 1; index >= 0; index--) {
            BaseComponent<?> child = children.get(index);
            if (child.isVisible() && child.blocksLowerInput() && child.isMouseOver(mouseX, mouseY)) {
                return index;
            }
        }
        return -1;
    }
}
