#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform ChromaticAberrationConfig {
    float Strength;
    float CenterFalloff;
};

out vec4 fragColor;

void main(){
    vec2 centered = texCoord - 0.5;

    // Aspect-corrected distance only for the falloff curve, so it reads as
    // circular regardless of screen shape; the offset direction stays in raw
    // UV space so the split doesn't skew with the aspect ratio.
    vec2 aspectCorrected = centered;
    aspectCorrected.x *= OutSize.x / OutSize.y;
    float dist = length(aspectCorrected);
    float falloff = pow(clamp(dist, 0.0, 1.0), max(CenterFalloff, 0.001));

    vec2 dir = normalize(centered + 1e-5);
    vec2 offset = dir * falloff * Strength * 0.02;

    float r = texture(InSampler, texCoord + offset).r;
    float g = texture(InSampler, texCoord).g;
    float b = texture(InSampler, texCoord - offset).b;
    float a = texture(InSampler, texCoord).a;

    fragColor = vec4(r, g, b, a);
}
