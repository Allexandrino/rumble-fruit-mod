#version 150
// glitch burst: horizontal bands tear sideways and the channels split
uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Time;
uniform float Intensity;
in vec2 texCoord;
out vec4 fragColor;
float hash(float n) { return fract(sin(n) * 43758.5453); }
void main() {
    vec2 uv = texCoord;
    float row = floor(uv.y * 48.0);
    float n = hash(row + floor(Time * 30.0) * 7.0);
    float band = step(0.92, n) * Intensity;
    uv.x += band * (hash(row * 1.7) - 0.5) * 0.25;
    vec3 col = texture(DiffuseSampler, uv).rgb;
    col.r = texture(DiffuseSampler, uv + vec2(0.004 * Intensity, 0.0)).r;
    col.b = texture(DiffuseSampler, uv - vec2(0.004 * Intensity, 0.0)).b;
    fragColor = vec4(col, 1.0);
}
