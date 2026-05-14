package com.sypztep.plateau.client.v2.ui.overlay;

import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.BaseContainerComponent;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.interaction.DragDrop;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Root overlay host for detachable panels.
 *
 * <p>Normal content renders first. Any open {@link DetachablePanel} descendants are then
 * rendered from this layer, so they are not clipped by intermediate FlowLayout or
 * ScrollContainer scissors.</p>
 */
@Environment(EnvType.CLIENT)
public class WindowLayer extends BaseContainerComponent<WindowLayer> {
    private BaseComponent<?> focusedFloating;
    private final List<DetachablePanel> zOrder = new ArrayList<>();

    public WindowLayer() {
        this.horizontalSizing = Sizing.fill();
        this.verticalSizing = Sizing.fill();
    }

    public WindowLayer content(BaseComponent<?> content) {
        children.clear();
        child(content);
        return this;
    }

    @Override
    protected void onMounted() {
        content().ifPresent(component -> component.mount(innerX(), innerY(), innerWidth(), innerHeight()));
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        DragDrop.beginFrame();

        boolean contentHoverBlocked = isContentHoverBlocked(mouseX, mouseY);
        content().ifPresent(component -> {
            component.mount(innerX(), innerY(), innerWidth(), innerHeight());
            component.extractRenderState(graphics,
                    contentHoverBlocked ? hoverSuppressedMouse() : mouseX,
                    contentHoverBlocked ? hoverSuppressedMouse() : mouseY,
                    delta);
        });

        List<DetachablePanel> panels = floatingPanels();
        syncZOrder(panels);
        for (DetachablePanel panel : zOrder) {
            if (!panel.isFloatingActive()) continue;
            panel.tickFloating(innerX(), innerY(), innerWidth(), innerHeight(), delta);
            panel.extractFloating(graphics, mouseX, mouseY, delta);
        }

        DragDrop.renderPreview(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        syncZOrder(floatingPanels());
        for (int i = zOrder.size() - 1; i >= 0; i--) {
            DetachablePanel panel = zOrder.get(i);
            if (!panel.isFloatingActive()) continue;

            if (panel.inputMode() == DetachablePanel.WindowInputMode.MODAL || panel.isFloatingMouseOver(event.x(), event.y())) {
                bringToFront(panel);
                if (panel.mouseClickedFloating(event, doubleClick)) {
                    focusedFloating = panel;
                    return true;
                }
                return panel.inputMode() != DetachablePanel.WindowInputMode.PINNED || panel.isFloatingMouseOver(event.x(), event.y());
            }
        }

        focusedFloating = null;
        return content().map(component -> component.mouseClicked(event, doubleClick)).orElse(false);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (DragDrop.active()) {
            return DragDrop.release(event);
        }

        syncZOrder(floatingPanels());
        if (focusedFloating instanceof DetachablePanel panel && panel.mouseReleasedFloating(event)) {
            return true;
        }

        for (int i = zOrder.size() - 1; i >= 0; i--) {
            DetachablePanel panel = zOrder.get(i);
            if (panel.isFloatingActive() && panel.isFloatingMouseOver(event.x(), event.y())) {
                return panel.mouseReleasedFloating(event);
            }
        }

        return content().map(component -> component.mouseReleased(event)).orElse(false);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (DragDrop.active()) return true;

        syncZOrder(floatingPanels());
        if (focusedFloating instanceof DetachablePanel panel && panel.mouseDraggedFloating(event, dragX, dragY)) {
            return true;
        }
        return content().map(component -> component.mouseDragged(event, dragX, dragY)).orElse(false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        syncZOrder(floatingPanels());
        for (int i = zOrder.size() - 1; i >= 0; i--) {
            DetachablePanel panel = zOrder.get(i);
            if (!panel.isFloatingActive()) continue;
            if (panel.inputMode() == DetachablePanel.WindowInputMode.MODAL || panel.isFloatingMouseOver(mouseX, mouseY)) {
                return panel.mouseScrolledFloating(mouseX, mouseY, hAmount, vAmount)
                        || panel.inputMode() != DetachablePanel.WindowInputMode.PINNED;
            }
        }

        return content().map(component -> component.mouseScrolled(mouseX, mouseY, hAmount, vAmount)).orElse(false);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        syncZOrder(floatingPanels());
        content().ifPresent(component -> component.mouseMoved(mouseX, mouseY));
        for (DetachablePanel panel : zOrder) {
            if (panel.isFloatingActive()) {
                panel.mouseMoved(mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (DragDrop.active() && event.key() == GLFW.GLFW_KEY_ESCAPE) {
            DragDrop.cancel();
            return true;
        }

        syncZOrder(floatingPanels());
        if (focusedFloating instanceof DetachablePanel panel && panel.keyPressedFloating(event)) {
            return true;
        }

        for (int i = zOrder.size() - 1; i >= 0; i--) {
            DetachablePanel panel = zOrder.get(i);
            if (panel.isFloatingActive() && panel.keyPressedFloating(event)) return true;
        }

        return content().map(component -> component.keyPressed(event)).orElse(false);
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        syncZOrder(floatingPanels());
        if (focusedFloating instanceof DetachablePanel panel && panel.charTypedFloating(event)) {
            return true;
        }

        for (int i = zOrder.size() - 1; i >= 0; i--) {
            DetachablePanel panel = zOrder.get(i);
            if (panel.isFloatingActive() && panel.charTypedFloating(event)) return true;
        }

        return content().map(component -> component.charTyped(event)).orElse(false);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return hitTest(mouseX, mouseY);
    }

    @Override
    public boolean hitTest(double mouseX, double mouseY) {
        if (super.hitTest(mouseX, mouseY)) return true;

        syncZOrder(floatingPanels());
        for (DetachablePanel panel : zOrder) {
            if (panel.isFloatingMouseOver(mouseX, mouseY)) return true;
        }
        return false;
    }

    private Optional<BaseComponent<?>> content() {
        return children.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(children.get(0));
    }

    private List<DetachablePanel> floatingPanels() {
        List<DetachablePanel> panels = new ArrayList<>();
        content().ifPresent(component -> collectPanels(component, panels));
        return panels;
    }

    private void collectPanels(BaseComponent<?> component, List<DetachablePanel> panels) {
        if (component instanceof DetachablePanel panel) {
            panels.add(panel);
        }

        if (component instanceof BaseContainerComponent<?> container) {
            for (BaseComponent<?> child : container.getChildren()) {
                collectPanels(child, panels);
            }
        }
    }

    private void syncZOrder(List<DetachablePanel> panels) {
        zOrder.removeIf(panel -> !panels.contains(panel));
        for (DetachablePanel panel : panels) {
            if (!zOrder.contains(panel)) {
                zOrder.add(panel);
            }
        }
    }

    private void bringToFront(DetachablePanel panel) {
        zOrder.remove(panel);
        zOrder.add(panel);
    }

    private boolean isContentHoverBlocked(int mouseX, int mouseY) {
        syncZOrder(floatingPanels());
        for (int index = zOrder.size() - 1; index >= 0; index--) {
            DetachablePanel panel = zOrder.get(index);
            if (!panel.isFloatingActive()) continue;
            if (panel.inputMode() == DetachablePanel.WindowInputMode.MODAL || panel.isFloatingMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void focusAfterInteraction(BaseComponent<?> child, int button) {
        if (child instanceof DetachablePanel panel) {
            focusedFloating = panel;
        } else {
            super.focusAfterInteraction(child, button);
        }
    }
}
