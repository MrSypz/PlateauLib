#version 330
#moj_import <minecraft:globals.glsl>

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform GlitchConfig {
    float Intensity;
    float BlockSize;
};

out vec4 fragColor;

float hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

void main(){
    // GameTime (Globals) is the day-cycle fraction (gameTime%24000 +
    // partialTick) / 24000, NOT a free-running seconds counter — it only
    // sweeps 0..1 once per 20-minute Minecraft day. Multiplying back by
    // 24000 recovers the actual (sub-tick-interpolated) tick count, which
    // is what advances once per real 1/20s tick as originally intended.
    float ticks = GameTime * 24000.0;

    float blockSize = max(BlockSize, 1.0);
    vec2 block = floor(texCoord * OutSize / blockSize);
    float seed = block.y + floor(ticks) * 137.0;

    float shiftTrigger = step(0.85, hash(seed + 91.7));
    float shift = (hash(seed) - 0.5) * Intensity * 0.1 * shiftTrigger;

    vec2 uv = texCoord + vec2(shift, 0.0);
    vec2 chromaOffset = vec2(shiftTrigger * Intensity * 0.005, 0.0);

    float r = texture(InSampler, uv + chromaOffset).r;
    float g = texture(InSampler, uv).g;
    float b = texture(InSampler, uv - chromaOffset).b;
    float a = texture(InSampler, uv).a;

    fragColor = vec4(r, g, b, a);
}
