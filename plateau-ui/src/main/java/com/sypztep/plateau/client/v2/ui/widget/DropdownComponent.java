package com.sypztep.plateau.client.v2.ui.widget;

import com.sypztep.plateau.client.v1.ui.core.RenderHelper;
import com.sypztep.plateau.client.v1.ui.core.UISounds;
import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Insets;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.core.Surface;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class DropdownComponent<T> extends BaseComponent {
    private final List<T> values = new ArrayList<>();
    private Function<T, Component> labeler;
    private int selectedIndex = 0;
    private boolean open = false;
    private boolean enabled = true;
    private Consumer<T> onChanged = ignored -> {};
    private float hoverProgress = 0f;
    private float openProgress = 0f;
    private boolean wasHovered = false;

    public DropdownComponent(List<T> values, Function<T, Component> labeler) {
        this.values.addAll(values);
        this.labeler = labeler;
        this.horizontalSizing = Sizing.fill();
        this.verticalSizing = Sizing.fixed(20);
    }

    @Override
    public int determineHorizontalContentSize(int space) { return Math.max(80 + padding.horizontal(), Math.max(0, space)); }

    @Override
    public int determineVerticalContentSize(int space) { return 20 + padding.vertical(); }

    @Override
    protected boolean isFocusable() { return enabled; }

    @Override
    public int renderClipBottomOutset() {
        return open ? values.size() * height + 2 : 0;
    }

    @Override
    public boolean rendersAboveSiblings() {
        return open;
    }

    @Override
    public boolean blocksLowerInput() {
        return open;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!visible) return false;
        if (super.isMouseOver(mouseX, mouseY)) return true;
        return open && mouseX >= x && mouseX < x + width && mouseY >= y + height && mouseY < y + height * (values.size() + 1);
    }

    @Override
    public boolean hitTest(double x, double y) {
        return isMouseOver(x, y);
    }

    @Override
    public void extract(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (width <= 0 || height <= 0) return;

        UITheme theme = UITheme.current();
        boolean hot = enabled && super.isMouseOver(mouseX, mouseY);
        if (hot && !wasHovered) UISounds.playHover();
        wasHovered = hot;
        hoverProgress = stepAnimation(hoverProgress, enabled && (hot || focused), 0.5f, delta);
        openProgress = stepAnimation(openProgress, open, 0.45f, delta);
        drawRow(g, selectedLabel(), y, hoverProgress, 0f, true);

        int arrowX = innerX() + innerWidth() - 12;
        int arrowY = innerY() + innerHeight() / 2 - 2;
        int arrowColor = enabled ? theme.text().primary() : theme.text().disabled();
        if (open) {
            g.fill(arrowX, arrowY + 3, arrowX + 7, arrowY + 4, arrowColor);
            g.fill(arrowX + 1, arrowY + 2, arrowX + 6, arrowY + 3, arrowColor);
            g.fill(arrowX + 2, arrowY + 1, arrowX + 5, arrowY + 2, arrowColor);
        } else {
            g.fill(arrowX, arrowY, arrowX + 7, arrowY + 1, arrowColor);
            g.fill(arrowX + 1, arrowY + 1, arrowX + 6, arrowY + 2, arrowColor);
            g.fill(arrowX + 2, arrowY + 2, arrowX + 5, arrowY + 3, arrowColor);
        }

        if (!open) return;

        for (int i = 0; i < values.size(); i++) {
            int rowY = y + height * (i + 1);
            boolean rowHot = mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + height;
            float optionHover = rowHot ? 1f : (i == selectedIndex ? 0.55f : openProgress * 0.15f);
            drawRow(g, labeler.apply(values.get(i)), rowY, optionHover, 0f, false);
        }
    }

    private void drawRow(GuiGraphicsExtractor g, Component label, int rowY, float hover, float press, boolean primary) {
        RenderHelper.ButtonColors colors = RenderHelper.buttonColors(enabled, hover, press);
        g.fillGradient(x + 2, rowY + 2, x + width - 2, rowY + height - 4, colors.bg(), colors.bgTop());
        g.outline(x, rowY, width, height, colors.border());
        g.outline(x + 1, rowY + 1, width - 2, height - 4, colors.outline());
        g.fill(x + 1, rowY + height - 3, x + width - 1, rowY + height - 1, colors.underline());
        int contentX = innerX() + 5;
        int contentY = rowY + padding.top();
        int contentW = Math.max(0, innerWidth() - (primary ? 21 : 10));
        int contentH = Math.max(0, height - padding.vertical());
        g.enableScissor(contentX, rowY, contentX + contentW, rowY + height);
        g.text(font, label, contentX, contentY + (contentH - font.lineHeight) / 2, colors.text(), true);
        g.disableScissor();
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!enabled || event.button() != 0 || !isMouseOver(event.x(), event.y())) return false;

        if (open && event.y() >= y + height) {
            int index = (int) ((event.y() - y) / height) - 1;
            if (index >= 0 && index < values.size()) selectedIndex(index);
            open = false;
            return true;
        }

        open = !open;
        UISounds.playClick();
        return true;
    }

    @Override
    public boolean onPointerClicked(MouseButtonEvent event, boolean doubleClick, double x, double y) {
        if (!enabled || event.button() != 0 || !isMouseOver(x, y)) return false;

        if (open && y >= this.y + height) {
            int index = (int) ((y - this.y) / height) - 1;
            if (index >= 0 && index < values.size()) selectedIndex(index);
            open = false;
            return true;
        }

        open = !open;
        UISounds.playClick();
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (!enabled || !focused) return false;

        return switch (event.key()) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> { open = !open; yield true; }
            case GLFW.GLFW_KEY_ESCAPE -> { open = false; yield true; }
            case GLFW.GLFW_KEY_UP -> { selectedIndex(selectedIndex - 1); yield true; }
            case GLFW.GLFW_KEY_DOWN -> { selectedIndex(selectedIndex + 1); yield true; }
            default -> false;
        };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (!enabled || !isMouseOver(mouseX, mouseY)) return false;
        selectedIndex(selectedIndex + (vAmount < 0 ? 1 : -1));
        return true;
    }

    private Component selectedLabel() {
        return values.isEmpty() ? Component.empty() : labeler.apply(values.get(selectedIndex));
    }

    public DropdownComponent<T> selectedIndex(int selectedIndex) {
        if (values.isEmpty()) {
            this.selectedIndex = 0;
            return this;
        }
        int next = Math.floorMod(selectedIndex, values.size());
        if (this.selectedIndex != next) {
            this.selectedIndex = next;
            UISounds.playClick();
            onChanged.accept(values.get(this.selectedIndex));
        }
        return this;
    }

    public DropdownComponent<T> value(T value) {
        int index = values.indexOf(value);
        if (index >= 0) selectedIndex(index);
        return this;
    }

    public T value() { return values.isEmpty() ? null : values.get(selectedIndex); }
    public int selectedIndex() { return selectedIndex; }
    public DropdownComponent<T> onChanged(Consumer<T> onChanged) { this.onChanged = onChanged != null ? onChanged : ignored -> {}; return this; }
    public DropdownComponent<T> labeler(Function<T, Component> labeler) { this.labeler = labeler; return this; }
    public DropdownComponent<T> enabled(boolean enabled) { this.enabled = enabled; return this; }
    public DropdownComponent<T> open(boolean open) { this.open = open; return this; }

    @Override public DropdownComponent<T> padding(Insets padding) { super.padding(padding); return this; }
    @Override public DropdownComponent<T> margins(Insets margins) { super.margins(margins); return this; }
    @Override public DropdownComponent<T> surface(Surface surface) { super.surface(surface); return this; }
    @Override public DropdownComponent<T> id(String id) { super.id(id); return this; }
    @Override public DropdownComponent<T> visible(boolean visible) { super.visible(visible); return this; }
    @Override public DropdownComponent<T> sizing(Sizing h, Sizing v) { super.sizing(h, v); return this; }
    @Override public DropdownComponent<T> sizing(Sizing both) { super.sizing(both); return this; }
}
