package com.sypztep.plateau.client.v1.ui.theme;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record UITheme(
        int screenBackground,
        Panel panel,
        Text text,
        Button button,
        Nav nav,
        Progress progress,
        Animation animation
) {
    public static UITheme current() {
        return UIThemeRegistry.INSTANCE.current();
    }

    public static final Codec<Integer> COLOR_CODEC = Codec.STRING.xmap(
            UITheme::parseColor,
            UITheme::formatColor
    );

    public static final Codec<UITheme> CODEC = RecordCodecBuilder.create(i -> i.group(
            COLOR_CODEC.fieldOf("screen_background").forGetter(UITheme::screenBackground),
            Panel.CODEC.fieldOf("panel").forGetter(UITheme::panel),
            Text.CODEC.fieldOf("text").forGetter(UITheme::text),
            Button.CODEC.fieldOf("button").forGetter(UITheme::button),
            Nav.CODEC.fieldOf("nav").forGetter(UITheme::nav),
            Progress.CODEC.fieldOf("progress").forGetter(UITheme::progress),
            Animation.CODEC.fieldOf("animation").forGetter(UITheme::animation)
    ).apply(i, UITheme::new));

    public record Panel(
            int bg,
            int bgHover,
            int border,
            int borderHover,
            int headerBg
    ) {
        public static final Codec<Panel> CODEC = RecordCodecBuilder.create(i -> i.group(
                COLOR_CODEC.fieldOf("bg").forGetter(Panel::bg),
                COLOR_CODEC.fieldOf("bg_hover").forGetter(Panel::bgHover),
                COLOR_CODEC.fieldOf("border").forGetter(Panel::border),
                COLOR_CODEC.fieldOf("border_hover").forGetter(Panel::borderHover),
                COLOR_CODEC.fieldOf("header_bg").forGetter(Panel::headerBg)
        ).apply(i, Panel::new));
    }

    public record Text(
            int primary,
            int secondary,
            int disabled,
            int accent
    ) {
        public static final Codec<Text> CODEC = RecordCodecBuilder.create(i -> i.group(
                COLOR_CODEC.fieldOf("primary").forGetter(Text::primary),
                COLOR_CODEC.fieldOf("secondary").forGetter(Text::secondary),
                COLOR_CODEC.fieldOf("disabled").forGetter(Text::disabled),
                COLOR_CODEC.fieldOf("accent").forGetter(Text::accent)
        ).apply(i, Text::new));
    }

    public record Button(
            ButtonState bg,
            ButtonState border,
            ButtonState underline,
            ButtonState outline,
            ButtonText text
    ) {
        public static final Codec<Button> CODEC = RecordCodecBuilder.create(i -> i.group(
                ButtonState.CODEC.fieldOf("bg").forGetter(Button::bg),
                ButtonState.CODEC.fieldOf("border").forGetter(Button::border),
                ButtonState.CODEC.fieldOf("underline").forGetter(Button::underline),
                ButtonState.CODEC.fieldOf("outline").forGetter(Button::outline),
                ButtonText.CODEC.fieldOf("text").forGetter(Button::text)
        ).apply(i, Button::new));
    }

    public record ButtonState(
            int normal,
            int hover,
            int pressed,
            int disabled
    ) {
        public static final Codec<ButtonState> CODEC = RecordCodecBuilder.create(i -> i.group(
                COLOR_CODEC.fieldOf("normal").forGetter(ButtonState::normal),
                COLOR_CODEC.fieldOf("hover").forGetter(ButtonState::hover),
                COLOR_CODEC.fieldOf("pressed").forGetter(ButtonState::pressed),
                COLOR_CODEC.fieldOf("disabled").forGetter(ButtonState::disabled)
        ).apply(i, ButtonState::new));
    }

    public record ButtonText(
            int normal,
            int hover
    ) {
        public static final Codec<ButtonText> CODEC = RecordCodecBuilder.create(i -> i.group(
                COLOR_CODEC.fieldOf("normal").forGetter(ButtonText::normal),
                COLOR_CODEC.fieldOf("hover").forGetter(ButtonText::hover)
        ).apply(i, ButtonText::new));
    }

    public record Nav(
            int bg,
            int indicator
    ) {
        public static final Codec<Nav> CODEC = RecordCodecBuilder.create(i -> i.group(
                COLOR_CODEC.fieldOf("bg").forGetter(Nav::bg),
                COLOR_CODEC.fieldOf("indicator").forGetter(Nav::indicator)
        ).apply(i, Nav::new));
    }

    public record Progress(
            int bg,
            int border,
            int fill
    ) {
        public static final Codec<Progress> CODEC = RecordCodecBuilder.create(i -> i.group(
                COLOR_CODEC.fieldOf("bg").forGetter(Progress::bg),
                COLOR_CODEC.fieldOf("border").forGetter(Progress::border),
                COLOR_CODEC.fieldOf("fill").forGetter(Progress::fill)
        ).apply(i, Progress::new));
    }

    public record Animation(
            float hoverSpeed,
            float hoverSpeedFast
    ) {
        public static final Codec<Animation> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.fieldOf("hover_speed").forGetter(Animation::hoverSpeed),
                Codec.FLOAT.fieldOf("hover_speed_fast").forGetter(Animation::hoverSpeedFast)
        ).apply(i, Animation::new));
    }

    private static int parseColor(String raw) {
        String s = raw.trim();

        if (s.startsWith("#")) {
            s = s.substring(1);
        }

        if (s.length() == 6) {
            return (int) (0xFF000000L | Long.parseLong(s, 16));
        }

        if (s.length() == 8) {
            return (int) Long.parseLong(s, 16);
        }

        throw new IllegalArgumentException("Invalid RGB/ARGB color: " + raw);
    }

    private static String formatColor(int color) {
        return String.format("#%08X", color);
    }
}