#version 330
#moj_import <plateau-postprocess:vfx_common.glsl>

uniform sampler2D InSampler;
uniform sampler2D InDepthSampler;
uniform sampler2D MaskSampler;

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

    // Restrict the edge to pixels the caller actually drew into the mask
    // group — VfxMaskGroups.draw() is the only thing that writes here.
    float masked = step(0.001, texture(MaskSampler, texCoord).a);
    edge *= masked;

    vec3 outColor = mix(diffuseColor.rgb, Color, edge);
    fragColor = vec4(outColor, diffuseColor.a);
}
