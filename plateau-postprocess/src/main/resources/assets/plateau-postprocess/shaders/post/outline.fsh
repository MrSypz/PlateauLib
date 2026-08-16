#version 330
#moj_import <plateau-postprocess:vfx_common.glsl>

uniform sampler2D InSampler;
uniform sampler2D InDepthSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform OutlineConfig {
    float Thickness;
    float DepthSensitivity;
    vec3 Color;
};

out vec4 fragColor;

void main(){
    vec4 diffuseColor = texture(InSampler, texCoord);
    vec2 oneTexel = (1.0 / InSize) * max(Thickness, 1.0);

    float edge = vfxDepthEdge(InDepthSampler, texCoord, oneTexel, DepthSensitivity);

    vec3 outColor = mix(diffuseColor.rgb, Color, edge);
    fragColor = vec4(outColor, diffuseColor.a);
}
