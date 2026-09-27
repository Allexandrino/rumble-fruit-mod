#version 150
// screen impact: the edges burn in the element's color, the center flashes,
// colors saturate and harden — everything on the buildup/peak/decay timeline
uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Intensity;
uniform vec3 Tint;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec2 uv = texCoord;
    vec3 col = texture(DiffuseSampler, uv).rgb;
    float aspect = OutSize.x / OutSize.y;
    vec2 d = uv - vec2(0.5);
    d.x *= aspect;
    float dist = length(d);
    // rgb split growing with the hit
    float split = Intensity * 0.006;
    col.r = texture(DiffuseSampler, uv + vec2(split, 0.0)).r;
    col.b = texture(DiffuseSampler, uv - vec2(split, 0.0)).b;
    // burning vignette in the element's color
    float vig = smoothstep(0.35, 0.85, dist);
    col += Tint * vig * Intensity * 0.8;
    // the core flash
    col += Tint * Intensity * Intensity * 0.35;
    // saturate + harden
    float lum = dot(col, vec3(0.299, 0.587, 0.114));
    col = mix(vec3(lum), col, 1.0 + 0.7 * Intensity);
    col = pow(col, vec3(1.0 + 0.25 * Intensity));
    fragColor = vec4(col, 1.0);
}
