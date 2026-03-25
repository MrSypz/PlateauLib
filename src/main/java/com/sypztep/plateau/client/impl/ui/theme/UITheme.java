package com.sypztep.plateau.client.impl.ui.theme;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record UITheme(
        // Screen
        int screenBackground,
        // Panel
        int panelBg, int panelBgHover, int panelBorder, int panelBorderHover, int panelHeaderBg,
        // Text
        int textPrimary, int textSecondary, int textDisabled, int textAccent,
        // Button
        int buttonBg, int buttonBgHover, int buttonBgPressed, int buttonBgDisabled,
        int buttonText, int buttonTextHover,
        // NavBar
        int navBg, int navIndicator,
        // Progress
        int progressBg, int progressBorder, int progressFill,
        // Animation
        float hoverSpeed, float hoverSpeedFast
) {
    // ═══════════════════════════════════════════
    // Built-in themes
    // ═══════════════════════════════════════════

    public static final UITheme DARK = new UITheme(
            0xF0101014,
            0xFF18191F, 0xFF22242C, 0xFF2E3040, 0xFF4A5068, 0xFF1E2028,
            0xFFE8E8EC, 0xFF9698A0, 0xFF505260, 0xFF5B9BD5,
            0xFF252730, 0xFF32354A, 0xFF1A1C24, 0xFF1A1C22,
            0xFF9698A0, 0xFFE8E8EC,
            0xFF1A1C22, 0xFF5B9BD5,
            0xFF282A34, 0xFF3E4050, 0xFF5BA85B,
            1f, 0.2f
    );

    public static final UITheme LEGACY = new UITheme(
            0xF0121212,
            0xFF1A1A1A, 0xFF222222, 0xFF424242, 0xFF6D6D6D, 0xFF212121,
            0xFFFFFFFF, 0xFFAAAAAA, 0xFF666666, 0xFFFFD700,
            0xFF2A2A2A, 0xFF3A3A3A, 0xFF1A1A1A, 0xFF1A1A1A,
            0xFFAAAAAA, 0xFFFFFFFF,
            0xFF1E1E1E, 0xFFFFCC00,
            0xFF333333, 0xFF555555, 0xFF7FBD3E,
            1f, 0.2f
    );

    // ═══════════════════════════════════════════
    // Current theme (global state)
    // ═══════════════════════════════════════════

    private static UITheme current = DARK;

    public static UITheme current() { return current; }
    public static void set(UITheme theme) { current = theme; }

    // ═══════════════════════════════════════════
    // Quick overrides
    // ═══════════════════════════════════════════

    public UITheme withAccent(int accent) {
        return from(this).textAccent(accent).navIndicator(accent).build();
    }

    // ═══════════════════════════════════════════
    // Builder for partial overrides
    // ═══════════════════════════════════════════

    public static Builder from(UITheme base) { return new Builder(base); }

    public static final class Builder {
        private int screenBackground;
        private int panelBg, panelBgHover, panelBorder, panelBorderHover, panelHeaderBg;
        private int textPrimary, textSecondary, textDisabled, textAccent;
        private int buttonBg, buttonBgHover, buttonBgPressed, buttonBgDisabled;
        private int buttonText, buttonTextHover;
        private int navBg, navIndicator;
        private int progressBg, progressBorder, progressFill;
        private float hoverSpeed, hoverSpeedFast;

        Builder(UITheme base) {
            this.screenBackground = base.screenBackground;
            this.panelBg = base.panelBg;
            this.panelBgHover = base.panelBgHover;
            this.panelBorder = base.panelBorder;
            this.panelBorderHover = base.panelBorderHover;
            this.panelHeaderBg = base.panelHeaderBg;
            this.textPrimary = base.textPrimary;
            this.textSecondary = base.textSecondary;
            this.textDisabled = base.textDisabled;
            this.textAccent = base.textAccent;
            this.buttonBg = base.buttonBg;
            this.buttonBgHover = base.buttonBgHover;
            this.buttonBgPressed = base.buttonBgPressed;
            this.buttonBgDisabled = base.buttonBgDisabled;
            this.buttonText = base.buttonText;
            this.buttonTextHover = base.buttonTextHover;
            this.navBg = base.navBg;
            this.navIndicator = base.navIndicator;
            this.progressBg = base.progressBg;
            this.progressBorder = base.progressBorder;
            this.progressFill = base.progressFill;
            this.hoverSpeed = base.hoverSpeed;
            this.hoverSpeedFast = base.hoverSpeedFast;
        }

        public Builder screenBackground(int v) { this.screenBackground = v; return this; }
        public Builder panelBg(int v) { this.panelBg = v; return this; }
        public Builder panelBgHover(int v) { this.panelBgHover = v; return this; }
        public Builder panelBorder(int v) { this.panelBorder = v; return this; }
        public Builder panelBorderHover(int v) { this.panelBorderHover = v; return this; }
        public Builder panelHeaderBg(int v) { this.panelHeaderBg = v; return this; }
        public Builder textPrimary(int v) { this.textPrimary = v; return this; }
        public Builder textSecondary(int v) { this.textSecondary = v; return this; }
        public Builder textDisabled(int v) { this.textDisabled = v; return this; }
        public Builder textAccent(int v) { this.textAccent = v; return this; }
        public Builder buttonBg(int v) { this.buttonBg = v; return this; }
        public Builder buttonBgHover(int v) { this.buttonBgHover = v; return this; }
        public Builder buttonBgPressed(int v) { this.buttonBgPressed = v; return this; }
        public Builder buttonBgDisabled(int v) { this.buttonBgDisabled = v; return this; }
        public Builder buttonText(int v) { this.buttonText = v; return this; }
        public Builder buttonTextHover(int v) { this.buttonTextHover = v; return this; }
        public Builder navBg(int v) { this.navBg = v; return this; }
        public Builder navIndicator(int v) { this.navIndicator = v; return this; }
        public Builder progressBg(int v) { this.progressBg = v; return this; }
        public Builder progressBorder(int v) { this.progressBorder = v; return this; }
        public Builder progressFill(int v) { this.progressFill = v; return this; }
        public Builder hoverSpeed(float v) { this.hoverSpeed = v; return this; }
        public Builder hoverSpeedFast(float v) { this.hoverSpeedFast = v; return this; }

        public UITheme build() {
            return new UITheme(
                    screenBackground,
                    panelBg, panelBgHover, panelBorder, panelBorderHover, panelHeaderBg,
                    textPrimary, textSecondary, textDisabled, textAccent,
                    buttonBg, buttonBgHover, buttonBgPressed, buttonBgDisabled,
                    buttonText, buttonTextHover,
                    navBg, navIndicator,
                    progressBg, progressBorder, progressFill,
                    hoverSpeed, hoverSpeedFast
            );
        }
    }
}
