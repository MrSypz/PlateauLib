// Shared across plateau-postprocess's built-in effects via #moj_import
// <plateau-postprocess:vfx_common.glsl> — same textual-splice mechanism
// vanilla uses for its own #moj_import <minecraft:globals.glsl> etc.

float vfxLuma(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

// GameTime (from #moj_import <minecraft:globals.glsl>) is the day-cycle
// fraction (gameTime%24000 + partialTick) / 24000, NOT a free-running
// seconds counter — it only sweeps 0..1 once per 20-minute Minecraft day.
// Multiplying back by 24000 recovers the actual (sub-tick-interpolated)
// tick count, which is what advances once per real 1/20s tick as expected.
// Takes GameTime as a parameter instead of referencing the global directly
// so this file doesn't assume the caller already imported globals.glsl.
float vfxTicksFromGameTime(float gameTime) {
    return gameTime * 24000.0;
}

// R sampled shifted toward +offset, B shifted toward -offset, G/A left at uv
// - the common "channel split" building block behind both glitch.fsh's block
// shift and chromatic_aberration.fsh's radial split; they only differ in how
// they compute offset, not in how they use it.
vec4 vfxChromaSplit(sampler2D tex, vec2 uv, vec2 offset) {
    float r = texture(tex, uv + offset).r;
    float g = texture(tex, uv).g;
    float b = texture(tex, uv - offset).b;
    float a = texture(tex, uv).a;
    return vec4(r, g, b, a);
}

// 4-tap depth-discontinuity edge detect — shared by outline.fsh and
// outline_masked.fsh, which are identical except for the mask step the
// masked variant applies to the result.
float vfxDepthEdge(sampler2D depthTex, vec2 uv, vec2 oneTexel, float sensitivity) {
    float center = texture(depthTex, uv).r;
    float left  = texture(depthTex, uv - vec2(oneTexel.x, 0.0)).r;
    float right = texture(depthTex, uv + vec2(oneTexel.x, 0.0)).r;
    float up    = texture(depthTex, uv - vec2(0.0, oneTexel.y)).r;
    float down  = texture(depthTex, uv + vec2(0.0, oneTexel.y)).r;
    float edge = abs(center - left) + abs(center - right) + abs(center - up) + abs(center - down);
    return clamp(edge * sensitivity, 0.0, 1.0);
}
