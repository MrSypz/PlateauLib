#version 330

uniform sampler2D MainSampler;
uniform sampler2D BloomSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BloomCompositeConfig {
    float Intensity;
};

out vec4 fragColor;

void main(){
    vec4 mainColor = texture(MainSampler, texCoord);
    vec4 bloomColor = texture(BloomSampler, texCoord);
    vec3 outColor = mainColor.rgb + bloomColor.rgb * Intensity;
    fragColor = vec4(outColor, mainColor.a);
}
