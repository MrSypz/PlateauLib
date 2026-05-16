package com.sypztep.plateau.client.v1.ui.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class UISounds {
    private UISounds() {}

    private static boolean globalEnabled = true;

    public static void setGlobalEnabled(boolean enabled) { globalEnabled = enabled; }
    public static boolean isGlobalEnabled() { return globalEnabled; }

    public static void playClick() {
        play(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.0f);
    }

    public static void playHover() {
        play(SoundEvents.NOTE_BLOCK_HAT.value(), 1.8f, 0.5f);
    }

    public static void playTabSwitch() {
        play(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.5f);
    }

    public static void playFocus() {
        play(SoundEvents.NOTE_BLOCK_HAT.value(), 2.0f, 0.3f);
    }

    public static void play(@Nullable SoundEvent sound, float pitch, float volume) {
        if (!globalEnabled || sound == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.getSoundSourceVolume(SoundSource.UI) <= 0f) return;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }
}
