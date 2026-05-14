package com.sypztep.plateau.client.v2.ui;

import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.widget.ButtonComponent;
import com.sypztep.plateau.client.v2.ui.widget.CheckBoxComponent;
import com.sypztep.plateau.client.v2.ui.widget.DropdownComponent;
import com.sypztep.plateau.client.v2.ui.widget.LabelComponent;
import com.sypztep.plateau.client.v2.ui.widget.SeparatorComponent;
import com.sypztep.plateau.client.v2.ui.widget.SliderButtonComponent;
import com.sypztep.plateau.client.v2.ui.widget.SpacerComponent;
import com.sypztep.plateau.client.v2.ui.widget.StringComponent;
import com.sypztep.plateau.client.v2.ui.widget.TextAreaComponent;
import com.sypztep.plateau.client.v2.ui.widget.TextComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Factory methods for leaf v2 UI components.
 * <pre>{@code
 * Components.label(Component.literal("Hello"))
 *     .color(0xFFFFFF)
 *     .sizing(Sizing.fill(), Sizing.fixed(12))
 * }</pre>
 */
@Environment(EnvType.CLIENT)
public final class WidgetComponents {

    private WidgetComponents() {}

    public static LabelComponent label(Component text)           { return new LabelComponent(text); }
    public static LabelComponent label(String text)              { return new LabelComponent(Component.literal(text)); }
    public static LabelComponent label(String text, int color)   { return new LabelComponent(Component.literal(text)).color(color); }

    public static TextComponent text(Component text) { return new TextComponent(text); }
    public static TextComponent text(String text)    { return new TextComponent(Component.literal(text)); }

    public static ButtonComponent button(Component label)                                          { return new ButtonComponent(label); }
    public static ButtonComponent button(String label)                                             { return new ButtonComponent(Component.literal(label)); }
    public static ButtonComponent button(Component label, Consumer<ButtonComponent> onClick) { return new ButtonComponent(label).onClick(onClick); }
    public static ButtonComponent button(String label,    Consumer<ButtonComponent> onClick) { return new ButtonComponent(Component.literal(label)).onClick(onClick); }

    public static SliderButtonComponent slider(Component label, double min, double max, double value) {
        return new SliderButtonComponent(label, min, max, value);
    }

    public static SliderButtonComponent slider(String label, double min, double max, double value) {
        return slider(Component.literal(label), min, max, value);
    }

    public static StringComponent string() {
        return new StringComponent();
    }

    public static StringComponent string(Component placeholder) {
        return new StringComponent(placeholder);
    }

    public static StringComponent string(String placeholder) {
        return new StringComponent(Component.literal(placeholder));
    }

    public static TextAreaComponent textArea() {
        return new TextAreaComponent();
    }

    public static TextAreaComponent textArea(Component placeholder) {
        return new TextAreaComponent(placeholder);
    }

    public static TextAreaComponent textArea(String placeholder) {
        return new TextAreaComponent(Component.literal(placeholder));
    }

    public static CheckBoxComponent checkbox(Component label, boolean checked) {
        return new CheckBoxComponent(label, checked);
    }

    public static CheckBoxComponent checkbox(String label, boolean checked) {
        return new CheckBoxComponent(Component.literal(label), checked);
    }

    public static <T> DropdownComponent<T> dropdown(List<T> values, Function<T, Component> labeler) {
        return new DropdownComponent<>(values, labeler);
    }

    public static DropdownComponent<String> dropdown(String... values) {
        return new DropdownComponent<>(List.of(values), Component::literal);
    }



    public static SpacerComponent spacer(int size)                      { return new SpacerComponent(size); }
    public static SpacerComponent spacer(Sizing horizontal, Sizing vertical) { return new SpacerComponent(horizontal, vertical); }
    public static SpacerComponent hSpacer()                             { return new SpacerComponent(Sizing.fill(), Sizing.fixed(0)); }
    public static SpacerComponent vSpacer()                             { return new SpacerComponent(Sizing.fixed(0), Sizing.fill()); }

    public static SeparatorComponent separator()                              { return new SeparatorComponent(); }
    public static SeparatorComponent separator(SeparatorComponent.Axis axis)  { return new SeparatorComponent(axis); }
    public static SeparatorComponent separator(int color)                     { return new SeparatorComponent().color(color); }

}
