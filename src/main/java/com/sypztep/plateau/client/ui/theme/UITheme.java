package com.sypztep.plateau.client.ui.theme;

public final class UITheme {
    private UITheme() {}

    // Screen
    public static final int SCREEN_BACKGROUND = 0xF0121212;

    // Panel
    public static final int PANEL_BG = 0xFF1A1A1A;
    public static final int PANEL_BG_HOVER = 0xFF222222;
    public static final int PANEL_BORDER = 0xFF424242;
    public static final int PANEL_BORDER_HOVER = 0xFF6D6D6D;
    public static final int PANEL_HEADER_BG = 0xFF212121;

    // Text
    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xFFAAAAAA;
    public static final int TEXT_DISABLED = 0xFF666666;
    public static final int TEXT_ACCENT = 0xFFFFD700;

    // Button
    public static final int BUTTON_BG = 0xFF2A2A2A;
    public static final int BUTTON_BG_HOVER = 0xFF3A3A3A;
    public static final int BUTTON_BG_PRESSED = 0xFF1A1A1A;
    public static final int BUTTON_BG_DISABLED = 0xFF1A1A1A;
    public static final int BUTTON_TEXT = 0xFFAAAAAA;
    public static final int BUTTON_TEXT_HOVER = 0xFFFFFFFF;

    // NavBar
    public static final int NAV_BG = 0xFF1E1E1E;
    public static final int NAV_INDICATOR = 0xFFFFCC00;

    // ProgressBar
    public static final int PROGRESS_BG = 0xFF333333;
    public static final int PROGRESS_BORDER = 0xFF555555;
    public static final int PROGRESS_FILL = 0xFF7FBD3E;

    // Debug overlay colors
    public static final int DEBUG_BOUNDS = 0xFFFF0000;       // Red — outer bounds
    public static final int DEBUG_CONTENT = 0xFF00FF00;      // Green — content area
    public static final int DEBUG_PADDING = 0x3300FF00;      // Green translucent — padding zone
    public static final int DEBUG_MARGIN = 0x330000FF;       // Blue translucent
    public static final int DEBUG_TEXT_BG = 0xCC000000;      // Label background
    public static final int DEBUG_TEXT = 0xFFFFFF00;         // Yellow text

    // Animation
    public static final float HOVER_SPEED = 1f;
    public static final float HOVER_SPEED_FAST = 0.2f;
}