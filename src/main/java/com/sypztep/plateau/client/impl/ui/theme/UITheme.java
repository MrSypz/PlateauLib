package com.sypztep.plateau.client.impl.ui.theme;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface UITheme {

    static UITheme current() { return ThemeHolder.INSTANCE; }
    static void set(UITheme theme) { ThemeHolder.INSTANCE = theme; }

    // Screen
    int screenBackground();

    // Panel
    int panelBg();
    int panelBgHover();
    int panelBorder();
    int panelBorderHover();
    int panelHeaderBg();

    // Text
    int textPrimary();
    int textSecondary();
    int textDisabled();
    int textAccent();

    // Button
    int buttonBg();
    int buttonBgHover();
    int buttonBgPressed();
    int buttonBgDisabled();
    int buttonText();
    int buttonTextHover();

    // NavBar
    int navBg();
    int navIndicator();

    // Progress
    int progressBg();
    int progressBorder();
    int progressFill();

    // Animation
    float hoverSpeed();
    float hoverSpeedFast();
}
