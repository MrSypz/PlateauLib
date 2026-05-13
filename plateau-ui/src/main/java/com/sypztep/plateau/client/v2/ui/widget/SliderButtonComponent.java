package com.sypztep.plateau.client.v2.ui.widget;

import com.sypztep.plateau.client.v1.ui.core.UISounds;
import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.function.DoubleConsumer;

@Environment(EnvType.CLIENT)
public class SliderButtonComponent extends BaseComponent<SliderButtonComponent> {
    private Component label;
    private double min;
    private double max;
    private double value;
    private boolean enabled = true;
    private boolean dragging = false;
    private DoubleConsumer onValueChange = ignored -> {};

    public SliderButtonComponent(Component label, double min, double max, double value) {
        this.label = label;
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
        this.value = clamp(value);
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
    public void extract(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (width <= 0 || height <= 0) return;

        UITheme theme = UITheme.current();
        boolean hovered = enabled && isMouseOver(mouseX, mouseY);
        int bg = enabled ? (hovered || focused ? theme.panel().bgHover() : theme.panel().bg()) : theme.button().bg().disabled();
        int border = hovered || focused ? theme.panel().borderHover() : theme.panel().border();

        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, bg);
        g.outline(x, y, width, height, border);

        int trackY = innerY() + innerHeight() - 5;
        int trackX = innerX() + 6;
        int trackW = Math.max(1, innerWidth() - 12);
        int fillW = (int) Math.round(normalized() * trackW);
        g.fill(trackX, trackY, trackX + trackW, trackY + 2, theme.progress().bg());
        g.fill(trackX, trackY, trackX + fillW, trackY + 2, enabled ? theme.progress().fill() : theme.text().disabled());

        int handleX = trackX + fillW - 3;
        g.fill(handleX, innerY() + 15, handleX + 6, innerY() + innerHeight() - 3, enabled ? 0xFFF3F2ED: theme.text().disabled());

        Component text = Component.literal(label.getString() + ": " + formatValue(value));
        int textColor = enabled ? theme.text().primary() : theme.text().disabled();
        g.enableScissor(innerX() + 4, innerY(), innerX() + innerWidth() - 4, innerY() + innerHeight());
        g.centeredText(font, text, innerX() + innerWidth() / 2, innerY() + 4, textColor);
        g.disableScissor();
    }
    // ERROR: THIS IS ARE NOW ISSUE it code smell I don't know the issue but this are should be the main interact of mouse event but! the thing it work are onPointerClicked() not this method
    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
//        if (!enabled || event.button() != 0 || !isMouseOver(event.x(), event.y())) return false;
//        dragging = true;
//        setValueFromMouse(event.x());
//        UISounds.playClick();
        return true;
    }

    @Override
    public boolean onPointerClicked(MouseButtonEvent event, boolean doubleClick, double x, double y) {
        if (!enabled || event.button() != 0 || !hitTest(x, y)) return false;
        dragging = true;
        setValueFromMouse(x);
        UISounds.playHover();
        return true;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (event.button() == 0) dragging = false;
        return false;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (!enabled || !dragging) return false;
        setValueFromMouse(event.x());
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (!enabled || !focused) return false;

        double step = (max - min) / 100.0;
        if (event.key() == GLFW.GLFW_KEY_LEFT) {
            value(value - step);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_RIGHT) {
            value(value + step);
            return true;
        }
        return false;
    }

    private void setValueFromMouse(double mouseX) {
        double n = (mouseX - (innerX() + 6)) / Math.max(1.0, innerWidth() - 12.0);
        value(min + (max - min) * n);
    }

    private double normalized() {
        if (max <= min) return 0.0;
        return (value - min) / (max - min);
    }

    private double clamp(double candidate) {
        return Math.max(min, Math.min(max, candidate));
    }

    private static String formatValue(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public SliderButtonComponent value(double value) {
        double next = clamp(value);
        if (Double.compare(this.value, next) != 0) {
            this.value = next;
            onValueChange.accept(this.value);
        }
        return this;
    }

    public double value() { return value; }
    public SliderButtonComponent range(double min, double max) { this.min = Math.min(min, max); this.max = Math.max(min, max); return value(value); }
    public SliderButtonComponent onValueChange(DoubleConsumer onValueChange) { this.onValueChange = onValueChange != null ? onValueChange : ignored -> {}; return this; }
    public SliderButtonComponent label(Component label) { this.label = label; return this; }
    public SliderButtonComponent enabled(boolean enabled) { this.enabled = enabled; return this; }
}
