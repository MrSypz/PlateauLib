package com.sypztep.plateau.client.v1.vfx.effects;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Parameters for one {@link DepthOfFieldEffect#requestFrame} call.
 *
 * @param focusDistance world-space distance from the camera, in blocks, that
 *                       stays sharp. The shader linearizes the raw depth
 *                       buffer against the camera's actual near/far planes
 *                       (computed fresh each frame from the player's render
 *                       distance) before comparing, so this is a real
 *                       distance, not a raw depth value — a naive raw-depth
 *                       {@code focusDepth} is nearly unusable in practice:
 *                       depth 0.9 is ~0.5 blocks from the camera, and depth
 *                       0.999 is already ~47 blocks, because standard
 *                       (non-reversed) depth buffers compress almost the
 *                       entire visible scene into the last sliver below 1.0
 * @param aperture       camera f-number (e.g. {@code 2.8} for a shallow,
 *                       "portrait mode" depth of field, {@code 16} for a
 *                       deep, almost-everything-sharp look). {@link
 *                       DepthOfFieldEffect} derives the in-focus zone width
 *                       from this the way a real lens would: a small
 *                       f-number (wide aperture) narrows it, a large
 *                       f-number (narrow aperture) widens it — this is a
 *                       game-friendly approximation of a thin-lens circle
 *                       of confusion, not physically exact (no focal
 *                       length/sensor size exists for a Minecraft camera)
 * @param blurRadius     box-blur radius (pixels) applied to the out-of-focus buffer
 * @param strength       overall blend strength of the blurred layer
 * @param autoFocus      when {@code true} (the default), {@code focusDistance} is
 *                       ignored and the shader focuses on whatever is at screen
 *                       center each frame — a depth-buffer sample, not a literal
 *                       raycast, but it answers the same question ("what's the
 *                       camera looking at") for free from data already rendered.
 *                       Set {@code false} to force a specific {@code focusDistance}
 *                       instead (e.g. a deliberate cutscene focus pull).
 */
@Environment(EnvType.CLIENT)
public record DepthOfFieldParams(float focusDistance, float aperture, float blurRadius, float strength, boolean autoFocus) {
    /** f/2.8, auto-focused on screen center — sharp subject, blurred background ("หน้าชัดหลังเบลอ"). */
    public static final DepthOfFieldParams DEFAULT = new DepthOfFieldParams(128.0f, 2.8f, 5.0f, 1.0f, true);
}
