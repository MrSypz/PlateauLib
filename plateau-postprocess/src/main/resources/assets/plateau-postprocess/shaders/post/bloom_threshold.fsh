#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BloomThresholdConfig {
    float Threshold;
};

out vec4 fragColor;

void main(){
    vec4 diffuseColor = texture(InSampler, texCoord);
    float luma = dot(diffuseColor.rgb, vec3(0.2126, 0.7152, 0.0722));
    float contribution = max(luma - Threshold, 0.0);
    float weight = contribution / max(luma, 0.0001);
    fragColor = vec4(diffuseColor.rgb * weight, diffuseColor.a);
}
