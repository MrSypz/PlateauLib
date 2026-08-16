#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BlurHConfig {
    float Radius;
};

in vec2 texCoord;

out vec4 fragColor;

// Adapted from vanilla's post/box_blur.fsh, split into a fixed-direction
// horizontal pass with its own uniform block name (BlurHConfig) so
// ManagedUniform can drive this pass independently from the vertical one —
// dynamic uniforms are matched by group name across every pass in a chain,
// so a shared "BlurConfig" name (as vanilla's own blur.json uses, with
// per-pass BlurDir baked statically) would force both directions to the same
// buffer. See BlurEffect/BloomEffect/DepthOfFieldEffect for the Java side.
void main() {
    vec2 oneTexel = 1.0 / InSize;
    vec2 sampleStep = oneTexel * vec2(1.0, 0.0);

    vec4 blurred = vec4(0.0);
    float actualRadius = round(max(Radius, 0.0));
    for (float a = -actualRadius + 0.5; a <= actualRadius; a += 2.0) {
        blurred += texture(InSampler, texCoord + sampleStep * a);
    }
    blurred += texture(InSampler, texCoord + sampleStep * actualRadius) / 2.0;
    fragColor = blurred / (actualRadius + 0.5);
}
