#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform VignetteConfig {
    float Intensity;
    float Roundness;
    vec3 Color;
};

out vec4 fragColor;

void main(){
    vec4 diffuseColor = texture(InSampler, texCoord);

    vec2 uv = (texCoord - 0.5) * 2.0;
    float aspect = OutSize.x / OutSize.y;
    uv.x *= mix(1.0, aspect, Roundness);

    float dist = length(uv);
    float falloff = smoothstep(0.3, 1.2, dist);
    float t = falloff * clamp(Intensity, 0.0, 1.0);

    vec3 outColor = mix(diffuseColor.rgb, Color, t);
    fragColor = vec4(outColor, diffuseColor.a);
}
