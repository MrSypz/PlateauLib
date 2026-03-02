package com.sypztep.plateau.client.impl.ui.theme;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ModernDarkTheme implements UITheme {
    public static final ModernDarkTheme INSTANCE = new ModernDarkTheme();
    private ModernDarkTheme() {}

    // Screen - deeper, subtly blue-tinted
    @Override public int screenBackground()   { return 0xF0101014; }

    // Panel - more depth, subtle blue undertone
    @Override public int panelBg()             { return 0xFF18191F; }
    @Override public int panelBgHover()        { return 0xFF22242C; }
    @Override public int panelBorder()         { return 0xFF2E3040; }
    @Override public int panelBorderHover()    { return 0xFF4A5068; }
    @Override public int panelHeaderBg()       { return 0xFF1E2028; }

    // Text - cleaner whites, teal accent
    @Override public int textPrimary()         { return 0xFFE8E8EC; }
    @Override public int textSecondary()       { return 0xFF9698A0; }
    @Override public int textDisabled()        { return 0xFF505260; }
    @Override public int textAccent()          { return 0xFF5B9BD5; }

    // Button - subtle gradient feel
    @Override public int buttonBg()            { return 0xFF252730; }
    @Override public int buttonBgHover()       { return 0xFF32354A; }
    @Override public int buttonBgPressed()     { return 0xFF1A1C24; }
    @Override public int buttonBgDisabled()    { return 0xFF1A1C22; }
    @Override public int buttonText()          { return 0xFF9698A0; }
    @Override public int buttonTextHover()     { return 0xFFE8E8EC; }

    // NavBar - teal indicator
    @Override public int navBg()               { return 0xFF1A1C22; }
    @Override public int navIndicator()        { return 0xFF5B9BD5; }

    // Progress
    @Override public int progressBg()          { return 0xFF282A34; }
    @Override public int progressBorder()      { return 0xFF3E4050; }
    @Override public int progressFill()        { return 0xFF5BA85B; }

    // Animation
    @Override public float hoverSpeed()        { return 1f; }
    @Override public float hoverSpeedFast()    { return 0.2f; }
}
