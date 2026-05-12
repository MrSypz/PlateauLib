package com.sypztep.plateau.client.v2.ui;

import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.widget.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Factory methods for leaf v2 UI components.
 * <pre>{@code
 * Components.label(Component.literal("Hello"))
 *     .color(0xFFFFFF)
 *     .sizing(Sizing.fill(), Sizing.fixed(12))
 * }</pre>
 */
@Environment(EnvType.CLIENT)
public final class Components {

    private Components() {}

    public static LabelComponent label(Component text)           { return new LabelComponent(text); }
    public static LabelComponent label(String text)              { return new LabelComponent(Component.literal(text)); }
    public static LabelComponent label(String text, int color)   { return new LabelComponent(Component.literal(text)).color(color); }

    public static TextComponent text(Component text) { return new TextComponent(text); }
    public static TextComponent text(String text)    { return new TextComponent(Component.literal(text)); }
    public static TabComponent tabs() { return new TabComponent(); }

    public static ButtonComponent button(Component label)                                          { return new ButtonComponent(label); }
    public static ButtonComponent button(String label)                                             { return new ButtonComponent(Component.literal(label)); }
    public static ButtonComponent button(Component label, Consumer<ButtonComponent> onClick) { return new ButtonComponent(label).onClick(onClick); }
    public static ButtonComponent button(String label,    Consumer<ButtonComponent> onClick) { return new ButtonComponent(Component.literal(label)).onClick(onClick); }



    public static SpacerComponent spacer(int size)                      { return new SpacerComponent(size); }
    public static SpacerComponent spacer(Sizing horizontal, Sizing vertical) { return new SpacerComponent(horizontal, vertical); }
    public static SpacerComponent hSpacer()                             { return new SpacerComponent(Sizing.fill(), Sizing.fixed(0)); }
    public static SpacerComponent vSpacer()                             { return new SpacerComponent(Sizing.fixed(0), Sizing.fill()); }

    public static SeparatorComponent separator()                              { return new SeparatorComponent(); }
    public static SeparatorComponent separator(SeparatorComponent.Axis axis)  { return new SeparatorComponent(axis); }
    public static SeparatorComponent separator(int color)                     { return new SeparatorComponent().color(color); }

    public static ScrollContainer scrollable(Sizing horizontal, Sizing vertical) {
        return new ScrollContainer(horizontal, vertical);
    }

    public static DialogComponent dialog() {
        return new DialogComponent();
    }

    public static DialogComponent dialog(String title) {
        return new DialogComponent().title(title);
    }

    public static DialogComponent dialog(Component title) {
        return new DialogComponent().title(title);
    }
}
