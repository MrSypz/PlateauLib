package com.sypztep.plateau.client.v1.vfx.mesh;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.ARGB;

/**
 * Procedural VFX geometry generators — draw directly into a
 * {@link VertexConsumer} obtained from a caller-owned {@code
 * MultiBufferSource} (batched with the rest of that draw call, no separate
 * one-off {@code Tesselator}/{@code MeshData} needed). Promoted from
 * knella's {@code VertexUtil}, which proved these shapes out in
 * {@code FireballRenderer}/{@code FireboltRenderer} — this is that same
 * known-good geometry, generalized out of knella's package so any
 * plateau-vfx consumer can reuse it instead of hand-rolling it per mod.
 */
@Environment(EnvType.CLIENT)
public final class PrimitiveMeshes {

    private PrimitiveMeshes() {}

    // ── Quad ───────────────────────────────────────────────────────────

    public static void quad(VertexConsumer buffer, PoseStack.Pose pose, int light,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             float u1, float v1, float u2, float v2,
                             int color) {
        buffer.addVertex(pose, x1, y1, z1).setUv(u1, v1).setColor(color).setLight(light);
        buffer.addVertex(pose, x2, y2, z2).setUv(u2, v1).setColor(color).setLight(light);
        buffer.addVertex(pose, x3, y3, z3).setUv(u2, v2).setColor(color).setLight(light);
        buffer.addVertex(pose, x4, y4, z4).setUv(u1, v2).setColor(color).setLight(light);
    }

    public static void quad(VertexConsumer buffer, PoseStack.Pose pose, int light,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             int color) {
        quad(buffer, pose, light,
                x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4,
                0f, 0f, 1f, 1f, color);
    }

    // ── Beam side ──────────────────────────────────────────────────────

    public static void beamSide(VertexConsumer buffer, PoseStack.Pose pose, int light,
                                 float x1, float z1, float x2, float z2, float height,
                                 float u1, float v1, float u2, float v2,
                                 int bottomColor, int topColor) {
        buffer.addVertex(pose, x1, 0, z1).setUv(u1, v2).setColor(bottomColor).setLight(light);
        buffer.addVertex(pose, x1, height, z1).setUv(u1, v1).setColor(topColor).setLight(light);
        buffer.addVertex(pose, x2, height, z2).setUv(u2, v1).setColor(topColor).setLight(light);
        buffer.addVertex(pose, x2, 0, z2).setUv(u2, v2).setColor(bottomColor).setLight(light);
    }

    // ── Rotating beam ──────────────────────────────────────────────────

    public static void rotatingBeam(VertexConsumer buffer, PoseStack.Pose pose, int light,
                                     float halfWidth, float height, float rotation,
                                     int bottomColor, int topColor) {
        float sin = (float) Math.sin(rotation);
        float cos = (float) Math.cos(rotation);

        float x1 = -halfWidth * cos + halfWidth * sin;
        float z1 = -halfWidth * sin - halfWidth * cos;
        float x2 = halfWidth * cos + halfWidth * sin;
        float z2 = halfWidth * sin - halfWidth * cos;
        float x3 = halfWidth * cos - halfWidth * sin;
        float z3 = halfWidth * sin + halfWidth * cos;
        float x4 = -halfWidth * cos - halfWidth * sin;
        float z4 = -halfWidth * sin + halfWidth * cos;

        float u1 = 0f, v1 = 0f;
        float u2 = 1f / 8f, v2 = 1f;

        beamSide(buffer, pose, light, x1, z1, x2, z2, height, u1, v1, u2, v2, bottomColor, topColor);
        beamSide(buffer, pose, light, x3, z3, x4, z4, height, u1, v1, u2, v2, bottomColor, topColor);
        beamSide(buffer, pose, light, x4, z4, x1, z1, height, u1, v1, u2, v2, bottomColor, topColor);
        beamSide(buffer, pose, light, x2, z2, x3, z3, height, u1, v1, u2, v2, bottomColor, topColor);
    }

    public static void rotatingBeam(VertexConsumer buffer, PoseStack.Pose pose, int light,
                                     float halfWidth, float height, float rotation,
                                     float r, float g, float b, float alpha, float endAlpha) {
        int bottomColor = ARGB.colorFromFloat(alpha, r, g, b);
        int topColor = ARGB.colorFromFloat(endAlpha, r, g, b);
        rotatingBeam(buffer, pose, light, halfWidth, height, rotation, bottomColor, topColor);
    }

    // ── Rotating sphere ────────────────────────────────────────────────

    /**
     * Core: spherical lat/lon UVs with scroll offset and tile factor.
     * uOffset animates the texture across the surface seamlessly (U wraps at 0→1).
     * tile controls how many times the texture repeats around the sphere.
     */
    public static void rotatingSphere(VertexConsumer buffer, PoseStack.Pose pose, int light,
                                       float radius, int color,
                                       float rotationY, float rotationX,
                                       int latSegments, int lonSegments,
                                       float uOffset, float tile) {
        for (int lat = 0; lat < latSegments; lat++) {
            float theta1 = lat * (float) Math.PI / latSegments;
            float theta2 = (lat + 1) * (float) Math.PI / latSegments;

            float v1 = (float) lat / latSegments;
            float v2 = (float) (lat + 1) / latSegments;

            for (int lon = 0; lon < lonSegments; lon++) {
                float phi1 = lon * 2f * (float) Math.PI / lonSegments;
                float phi2 = (lon + 1) * 2f * (float) Math.PI / lonSegments;

                // U wraps seamlessly: lon==lonSegments lands back at lon==0
                float u1 = ((float) lon / lonSegments + uOffset) * tile;
                float u2 = ((float) (lon + 1) / lonSegments + uOffset) * tile;

                float[] p1 = spherePoint(radius, theta1, phi1, rotationY, rotationX);
                float[] p2 = spherePoint(radius, theta1, phi2, rotationY, rotationX);
                float[] p3 = spherePoint(radius, theta2, phi2, rotationY, rotationX);
                float[] p4 = spherePoint(radius, theta2, phi1, rotationY, rotationX);

                quad(buffer, pose, light,
                        p1[0], p1[1], p1[2],
                        p2[0], p2[1], p2[2],
                        p3[0], p3[1], p3[2],
                        p4[0], p4[1], p4[2],
                        u1, v1, u2, v2, color);
            }
        }
    }

    /** Convenience: int color, no scroll. */
    public static void rotatingSphere(VertexConsumer buffer, PoseStack.Pose pose, int light,
                                       float radius, int color,
                                       float rotationY, float rotationX,
                                       int latSegments, int lonSegments) {
        rotatingSphere(buffer, pose, light, radius, color,
                rotationY, rotationX, latSegments, lonSegments, 0f, 1f);
    }

    /** Convenience: float RGBA, with scroll + tile. */
    public static void rotatingSphere(VertexConsumer buffer, PoseStack.Pose pose, int light,
                                       float radius, float r, float g, float b, float alpha,
                                       float rotationY, float rotationX,
                                       int latSegments, int lonSegments,
                                       float uOffset, float tile) {
        rotatingSphere(buffer, pose, light, radius,
                ARGB.colorFromFloat(alpha, r, g, b),
                rotationY, rotationX, latSegments, lonSegments, uOffset, tile);
    }

    /** Convenience: float RGBA, no scroll (backward compat). */
    public static void rotatingSphere(VertexConsumer buffer, PoseStack.Pose pose, int light,
                                       float radius, float r, float g, float b, float alpha,
                                       float rotationY, float rotationX,
                                       int latSegments, int lonSegments) {
        rotatingSphere(buffer, pose, light, radius,
                ARGB.colorFromFloat(alpha, r, g, b),
                rotationY, rotationX, latSegments, lonSegments, 0f, 1f);
    }

    // ── Depth hull ─────────────────────────────────────────────────────

    /**
     * Same tessellation as {@link #rotatingSphere} but with reversed winding, so a
     * back-face-culling pipeline renders only the hemisphere facing AWAY from the
     * camera. For depth-only VFX footprints: stamping the back hull's depth occludes
     * later passes (clouds/weather) behind the effect while never clipping the
     * effect's own non-depth-writing colour layers — they are always nearer than the
     * back hull, so the result is independent of the order the submits are drawn in.
     * Also used with an opaque pipeline as a backdrop canvas — tint via {@code color}.
     */
    public static void sphereBackHull(VertexConsumer buffer, PoseStack.Pose pose, int light,
                                       float radius, int latSegments, int lonSegments, int color) {
        for (int lat = 0; lat < latSegments; lat++) {
            float theta1 = lat * (float) Math.PI / latSegments;
            float theta2 = (lat + 1) * (float) Math.PI / latSegments;

            float v1 = (float) lat / latSegments;
            float v2 = (float) (lat + 1) / latSegments;

            for (int lon = 0; lon < lonSegments; lon++) {
                float phi1 = lon * 2f * (float) Math.PI / lonSegments;
                float phi2 = (lon + 1) * 2f * (float) Math.PI / lonSegments;

                float[] p1 = spherePoint(radius, theta1, phi1, 0f, 0f);
                float[] p2 = spherePoint(radius, theta1, phi2, 0f, 0f);
                float[] p3 = spherePoint(radius, theta2, phi2, 0f, 0f);
                float[] p4 = spherePoint(radius, theta2, phi1, 0f, 0f);

                quad(buffer, pose, light,
                        p4[0], p4[1], p4[2],
                        p3[0], p3[1], p3[2],
                        p2[0], p2[1], p2[2],
                        p1[0], p1[1], p1[2],
                        0f, 0f, 1f, 1f, color);
            }
        }
    }

    // ── Hoop ───────────────────────────────────────────────────────────

    /**
     * A cylindrical band around the local Y axis — a "barrel hoop". Unlike a flat
     * annulus it never vanishes edge-on: from the side it reads as a bright bar,
     * from the pole as a circle outline. U runs around the circumference (with the
     * same scroll/tile convention as {@link #rotatingSphere}) so a texture can be
     * made to race around the ring; V runs across the band's height.
     */
    public static void hoop(VertexConsumer buffer, PoseStack.Pose pose, int light,
                             float radius, float halfHeight, int segments,
                             float uOffset, float tile, int color) {
        float step = (float) (Math.PI * 2.0) / segments;
        for (int i = 0; i < segments; i++) {
            float a1 = i * step;
            float a2 = a1 + step;
            float cos1 = (float) Math.cos(a1), sin1 = (float) Math.sin(a1);
            float cos2 = (float) Math.cos(a2), sin2 = (float) Math.sin(a2);

            float u1 = ((float) i / segments + uOffset) * tile;
            float u2 = ((float) (i + 1) / segments + uOffset) * tile;

            quad(buffer, pose, light,
                    cos1 * radius, halfHeight, sin1 * radius,
                    cos2 * radius, halfHeight, sin2 * radius,
                    cos2 * radius, -halfHeight, sin2 * radius,
                    cos1 * radius, -halfHeight, sin1 * radius,
                    u1, 0f, u2, 1f, color);
        }
    }

    public static void hoop(VertexConsumer buffer, PoseStack.Pose pose, int light,
                             float radius, float halfHeight, int segments,
                             float uOffset, float tile,
                             float r, float g, float b, float alpha) {
        hoop(buffer, pose, light, radius, halfHeight, segments, uOffset, tile,
                ARGB.colorFromFloat(alpha, r, g, b));
    }

    // ── Rotating box ───────────────────────────────────────────────────

    public static void rotatingBox(VertexConsumer buffer, PoseStack.Pose pose, int light,
                                    float halfSize, int color) {
        float h = halfSize;
        quad(buffer, pose, light, -h, h, -h, h, h, -h, h, h, h, -h, h, h, color);
        quad(buffer, pose, light, -h, -h, h, h, -h, h, h, -h, -h, -h, -h, -h, color);
        quad(buffer, pose, light, h, -h, -h, h, -h, h, h, h, h, h, h, -h, color);
        quad(buffer, pose, light, -h, -h, h, -h, -h, -h, -h, h, -h, -h, h, h, color);
        quad(buffer, pose, light, -h, -h, h, h, -h, h, h, h, h, -h, h, h, color);
        quad(buffer, pose, light, h, -h, -h, -h, -h, -h, -h, h, -h, h, h, -h, color);
    }

    public static void rotatingBox(VertexConsumer buffer, PoseStack.Pose pose, int light,
                                    float halfSize, float r, float g, float b, float alpha) {
        rotatingBox(buffer, pose, light, halfSize, ARGB.colorFromFloat(alpha, r, g, b));
    }

    // ── Billboard circle ───────────────────────────────────────────────

    public static void billboardCircle(VertexConsumer buffer, PoseStack.Pose pose, int light,
                                        float radius, int color, int segments) {
        billboardCircle(buffer, pose, light, radius, color, segments, 0f);
    }

    /**
     * Disc UVs spin about the centre by {@code uvRotation}. Rotation is
     * length-preserving, so {@code length(uv - 0.5) * 2} stays a true normalized
     * radius in the fragment shader — a shader can keep doing radial falloff math
     * on these UVs while any texture it samples visibly turns. Deliberately no UV
     * scale knob: scaling would break that radius invariant. Tile in the shader
     * instead.
     */
    public static void billboardCircle(VertexConsumer buffer, PoseStack.Pose pose, int light,
                                        float radius, int color, int segments, float uvRotation) {
        float step = (float) (Math.PI * 2.0) / segments;
        float cr = (float) Math.cos(uvRotation);
        float sr = (float) Math.sin(uvRotation);
        for (int i = 0; i < segments; i++) {
            float a1 = i * step;
            float a2 = a1 + step;
            float cos1 = (float) Math.cos(a1), sin1 = (float) Math.sin(a1);
            float cos2 = (float) Math.cos(a2), sin2 = (float) Math.sin(a2);

            float u1 = 0.5f + 0.5f * (cos1 * cr - sin1 * sr);
            float v1 = 0.5f + 0.5f * (cos1 * sr + sin1 * cr);
            float u2 = 0.5f + 0.5f * (cos2 * cr - sin2 * sr);
            float v2 = 0.5f + 0.5f * (cos2 * sr + sin2 * cr);

            buffer.addVertex(pose, 0, 0, 0).setUv(0.5f, 0.5f).setColor(color).setLight(light);
            buffer.addVertex(pose, cos1 * radius, sin1 * radius, 0).setUv(u1, v1).setColor(color).setLight(light);
            buffer.addVertex(pose, cos2 * radius, sin2 * radius, 0).setUv(u2, v2).setColor(color).setLight(light);
            buffer.addVertex(pose, 0, 0, 0).setUv(0.5f, 0.5f).setColor(color).setLight(light);
        }
    }

    // ── Internal ───────────────────────────────────────────────────────

    private static float[] spherePoint(float radius, float theta, float phi,
                                        float rotationY, float rotationX) {
        float x = radius * (float) (Math.sin(theta) * Math.cos(phi));
        float y = radius * (float) Math.cos(theta);
        float z = radius * (float) (Math.sin(theta) * Math.sin(phi));

        float sinY = (float) Math.sin(rotationY);
        float cosY = (float) Math.cos(rotationY);
        float rx = x * cosY - z * sinY;
        float rz = x * sinY + z * cosY;

        float sinX = (float) Math.sin(rotationX);
        float cosX = (float) Math.cos(rotationX);
        float ry = y * cosX - rz * sinX;
        float fz = y * sinX + rz * cosX;

        return new float[]{rx, ry, fz};
    }
}
