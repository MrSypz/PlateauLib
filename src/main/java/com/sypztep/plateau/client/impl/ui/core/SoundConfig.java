package com.sypztep.plateau.client.impl.ui.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class SoundConfig {
    private boolean hoverEnabled;
    private boolean clickEnabled;
    @Nullable private SoundEvent hoverSound = SoundEvents.NOTE_BLOCK_HAT.value();
    @Nullable private SoundEvent clickSound = SoundEvents.UI_BUTTON_CLICK.value();
    private float hoverPitch = 1.8f;
    private float hoverVolume = 0.5f;
    private float clickPitch = 1.0f;
    private float clickVolume = 1.0f;

    private SoundConfig(boolean hoverEnabled, boolean clickEnabled) {
        this.hoverEnabled = hoverEnabled;
        this.clickEnabled = clickEnabled;
    }

    public static SoundConfig silent() {
        return new SoundConfig(false, false);
    }

    public static SoundConfig button() {
        return new SoundConfig(true, true);
    }

    public static SoundConfig subtle() {
        SoundConfig c = new SoundConfig(true, true);
        c.hoverVolume = 0.3f;
        c.clickVolume = 0.5f;
        return c;
    }

    public void playHover() {
        if (hoverEnabled) UISounds.play(hoverSound, hoverPitch, hoverVolume);
    }

    public void playClick() {
        if (clickEnabled) UISounds.play(clickSound, clickPitch, clickVolume);
    }

    public boolean isHoverEnabled() { return hoverEnabled; }
    public boolean isClickEnabled() { return clickEnabled; }

    public SoundConfig setHoverEnabled(boolean enabled) { this.hoverEnabled = enabled; return this; }
    public SoundConfig setClickEnabled(boolean enabled) { this.clickEnabled = enabled; return this; }
    public SoundConfig setHoverSound(@Nullable SoundEvent sound, float pitch, float volume) {
        this.hoverSound = sound;
        this.hoverPitch = pitch;
        this.hoverVolume = volume;
        return this;
    }
    public SoundConfig setClickSound(@Nullable SoundEvent sound, float pitch, float volume) {
        this.clickSound = sound;
        this.clickPitch = pitch;
        this.clickVolume = volume;
        return this;
    }
}
