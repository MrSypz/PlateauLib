package com.sypztep.plateau.client.v1.vfx;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Optional;

/**
 * Shared "why didn't this effect run / what did it cost" surface, populated by
 * {@link VfxManager} for every registered effect — debug overlays read this
 * instead of each consumer hand-rolling error fields and throttled logging.
 */
@Environment(EnvType.CLIENT)
public interface VfxDiagnostics {

    /**
     * Why the effect skipped its most recent frame — e.g. {@code "gated by when()"},
     * {@code "prepare produced nothing"}, {@code "post chain not loaded: <id>"},
     * {@code "error: <exception>"} — or empty if it ran.
     */
    Optional<String> lastSkipReason();

    /** Combined prepare + contribute cost of the most recent frame the effect ran, in nanoseconds. */
    long lastFrameCostNanos();
}
