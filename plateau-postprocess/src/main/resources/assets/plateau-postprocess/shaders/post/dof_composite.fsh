#version 330

uniform sampler2D MainSampler;
uniform sampler2D MainDepthSampler;
uniform sampler2D BlurredSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

// Near/Far are the camera's actual projection planes (Near is the fixed
// Camera.PROJECTION_Z_NEAR constant; Far varies with the player's render
// distance/cloud range) — recomputed and fed in fresh every frame by
// DepthOfFieldEffect since no post-chain uniform exposes them directly.
// FocusDistance is a real world-space distance in blocks, not raw depth: raw
// depth is so non-linear that depth=0.9 is ~0.5 blocks from the camera and
// depth=0.999 is already ~47 blocks — unusable as a tuning parameter, hence
// linearizing here instead. Aperture is a camera f-number (2.8 = shallow
// "portrait" DOF, 16 = deep focus) that derives the in-focus zone width the
// way a real lens would; see DepthOfFieldParams for the formula.
layout(std140) uniform DepthOfFieldConfig {
    float Near;
    float Far;
    float FocusDistance;
    float Aperture;
    float Strength;
    float AutoFocus;
};

out vec4 fragColor;

float linearizeDepth(float rawDepth) {
    float ndc = rawDepth * 2.0 - 1.0;
    return (2.0 * Near * Far) / (Far + Near - ndc * (Far - Near));
}

void main(){
    vec4 sharp = texture(MainSampler, texCoord);
    vec4 blurred = texture(BlurredSampler, texCoord);
    float linearDepth = linearizeDepth(texture(MainDepthSampler, texCoord).r);

    // AutoFocus > 0.5: focus on whatever the camera is actually looking at
    // (screen-center depth), like a camera's autofocus — not a literal
    // raycast, but the depth buffer already tells us exactly what's under
    // the crosshair for free, which is both cheaper and pixel-accurate.
    // AutoFocus <= 0.5: honor the manually-set FocusDistance (e.g. for a
    // deliberate cutscene focus pull instead of tracking the crosshair).
    float focusDistance = AutoFocus > 0.5
        ? linearizeDepth(texture(MainDepthSampler, vec2(0.5)).r)
        : FocusDistance;

    float focusRange = max(focusDistance * Aperture / 4.0, 0.5);
    float coc = clamp(abs(linearDepth - focusDistance) / focusRange, 0.0, 1.0);
    float blend = coc * clamp(Strength, 0.0, 1.0);

    vec3 outColor = mix(sharp.rgb, blurred.rgb, blend);
    fragColor = vec4(outColor, sharp.a);
}
