#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BlurVConfig {
    float Radius;
};

in vec2 texCoord;

out vec4 fragColor;

// Vertical counterpart of box_blur_h.fsh — see that file for why the
// direction is fixed per-shader instead of a shared BlurDir uniform.
void main() {
    vec2 oneTexel = 1.0 / InSize;
    vec2 sampleStep = oneTexel * vec2(0.0, 1.0);

    vec4 blurred = vec4(0.0);
    float actualRadius = round(max(Radius, 0.0));
    for (float a = -actualRadius + 0.5; a <= actualRadius; a += 2.0) {
        blurred += texture(InSampler, texCoord + sampleStep * a);
    }
    blurred += texture(InSampler, texCoord + sampleStep * actualRadius) / 2.0;
    fragColor = blurred / (actualRadius + 0.5);
}
