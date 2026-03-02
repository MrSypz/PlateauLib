package com.sypztep.plateau.client.impl.ui.theme;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class LegacyTheme implements UITheme {
    public static final LegacyTheme INSTANCE = new LegacyTheme();
    private LegacyTheme() {}

    @Override public int screenBackground()   { return 0xF0121212; }
    @Override public int panelBg()             { return 0xFF1A1A1A; }
    @Override public int panelBgHover()        { return 0xFF222222; }
    @Override public int panelBorder()         { return 0xFF424242; }
    @Override public int panelBorderHover()    { return 0xFF6D6D6D; }
    @Override public int panelHeaderBg()       { return 0xFF212121; }
    @Override public int textPrimary()         { return 0xFFFFFFFF; }
    @Override public int textSecondary()       { return 0xFFAAAAAA; }
    @Override public int textDisabled()        { return 0xFF666666; }
    @Override public int textAccent()          { return 0xFFFFD700; }
    @Override public int buttonBg()            { return 0xFF2A2A2A; }
    @Override public int buttonBgHover()       { return 0xFF3A3A3A; }
    @Override public int buttonBgPressed()     { return 0xFF1A1A1A; }
    @Override public int buttonBgDisabled()    { return 0xFF1A1A1A; }
    @Override public int buttonText()          { return 0xFFAAAAAA; }
    @Override public int buttonTextHover()     { return 0xFFFFFFFF; }
    @Override public int navBg()               { return 0xFF1E1E1E; }
    @Override public int navIndicator()        { return 0xFFFFCC00; }
    @Override public int progressBg()          { return 0xFF333333; }
    @Override public int progressBorder()      { return 0xFF555555; }
    @Override public int progressFill()        { return 0xFF7FBD3E; }
    @Override public float hoverSpeed()        { return 1f; }
    @Override public float hoverSpeedFast()    { return 0.2f; }
}
