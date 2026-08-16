#version 330
#moj_import <minecraft:globals.glsl>
#moj_import <plateau-postprocess:vfx_common.glsl>

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform FilmGrainConfig {
    float Intensity;
    float Size;
};

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453123);
}

void main(){
    vec4 diffuseColor = texture(InSampler, texCoord);

    float ticks = vfxTicksFromGameTime(GameTime);

    vec2 grainCoord = texCoord * OutSize / max(Size, 0.01);
    float grain = hash(grainCoord + ticks * 9.77) - 0.5;

    vec3 outColor = diffuseColor.rgb + grain * Intensity;
    fragColor = vec4(clamp(outColor, 0.0, 1.0), diffuseColor.a);
}
