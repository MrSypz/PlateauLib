#version 330
#moj_import <plateau-postprocess:vfx_common.glsl>

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform ColorGradingConfig {
    float Saturation;
    float Contrast;
    float Brightness;
    float Temperature;
};

out vec4 fragColor;

void main(){
    vec4 diffuseColor = texture(InSampler, texCoord);
    vec3 color = diffuseColor.rgb;

    color += Brightness;
    color = (color - 0.5) * Contrast + 0.5;

    color = mix(vec3(vfxLuma(color)), color, Saturation);

    color.r += Temperature * 0.1;
    color.b -= Temperature * 0.1;

    fragColor = vec4(clamp(color, 0.0, 1.0), diffuseColor.a);
}
