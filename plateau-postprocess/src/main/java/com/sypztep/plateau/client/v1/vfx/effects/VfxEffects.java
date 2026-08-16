package com.sypztep.plateau.client.v1.vfx.effects;

import com.sypztep.plateau.client.v1.vfx.VfxManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Registry of PlateauLib's built-in post effects — the Effect Graph's effect
 * library. Registered once by {@code PlateauPostprocessClient} at client
 * init; consumers never call {@link VfxManager#register} for these, they
 * just call {@code VfxEffects.<effect>().requestFrame(weight, params)} every
 * frame they want the effect active.
 *
 * <p>Registration order (via priority) is a sensible default composite
 * pipeline — light-shaping effects (bloom, depth of field, blur) run before
 * scene-analysis (outline) and finally screen-space color/distortion
 * touches (grading, aberration, glitch, grain, vignette).
 */
@Environment(EnvType.CLIENT)
public final class VfxEffects {
    private static final BloomEffect BLOOM = new BloomEffect();
    private static final DepthOfFieldEffect DEPTH_OF_FIELD = new DepthOfFieldEffect();
    private static final BlurEffect BLUR = new BlurEffect();
    private static final OutlineEffect OUTLINE = new OutlineEffect();
    private static final ColorGradingEffect COLOR_GRADING = new ColorGradingEffect();
    private static final ChromaticAberrationEffect CHROMATIC_ABERRATION = new ChromaticAberrationEffect();
    private static final GlitchEffect GLITCH = new GlitchEffect();
    private static final FilmGrainEffect FILM_GRAIN = new FilmGrainEffect();
    private static final VignetteEffect VIGNETTE = new VignetteEffect();

    private VfxEffects() {}

    public static BloomEffect bloom() {
        return BLOOM;
    }

    public static DepthOfFieldEffect depthOfField() {
        return DEPTH_OF_FIELD;
    }

    public static BlurEffect blur() {
        return BLUR;
    }

    public static OutlineEffect outline() {
        return OUTLINE;
    }

    public static ColorGradingEffect colorGrading() {
        return COLOR_GRADING;
    }

    public static ChromaticAberrationEffect chromaticAberration() {
        return CHROMATIC_ABERRATION;
    }

    public static GlitchEffect glitch() {
        return GLITCH;
    }

    public static FilmGrainEffect filmGrain() {
        return FILM_GRAIN;
    }

    public static VignetteEffect vignette() {
        return VIGNETTE;
    }

    /** Registers every built-in effect with {@link VfxManager}. Called once at client init. */
    public static void registerAll() {
        VfxManager.register(BLOOM).priority(90);
        VfxManager.register(DEPTH_OF_FIELD).priority(80);
        VfxManager.register(BLUR).priority(70);
        VfxManager.register(OUTLINE).priority(60);
        VfxManager.register(COLOR_GRADING).priority(50);
        VfxManager.register(CHROMATIC_ABERRATION).priority(40);
        VfxManager.register(GLITCH).priority(30);
        VfxManager.register(FILM_GRAIN).priority(20);
        VfxManager.register(VIGNETTE).priority(10);
    }
}
