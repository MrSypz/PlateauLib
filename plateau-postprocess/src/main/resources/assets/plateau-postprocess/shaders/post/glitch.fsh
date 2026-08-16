#version 330
#moj_import <minecraft:globals.glsl>
#moj_import <plateau-postprocess:vfx_common.glsl>

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
    float ticks = vfxTicksFromGameTime(GameTime);

    float blockSize = max(BlockSize, 1.0);
    vec2 block = floor(texCoord * OutSize / blockSize);
    float seed = block.y + floor(ticks) * 137.0;

    float shiftTrigger = step(0.85, hash(seed + 91.7));
    float shift = (hash(seed) - 0.5) * Intensity * 0.1 * shiftTrigger;

    vec2 uv = texCoord + vec2(shift, 0.0);
    vec2 chromaOffset = vec2(shiftTrigger * Intensity * 0.005, 0.0);

    fragColor = vfxChromaSplit(InSampler, uv, chromaOffset);
}
