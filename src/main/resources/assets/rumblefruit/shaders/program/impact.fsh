#version 150
// screen impact: rgb-split, вспышка в цвете стихии и насыщенность —
// БЕЗ круговой виньетки (кольца убраны по просьбе)
uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Intensity;
uniform vec3 Tint;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec2 uv = texCoord;
    vec3 col = texture(DiffuseSampler, uv).rgb;
    // rgb split growing with the hit
    float split = Intensity * 0.006;
    col.r = texture(DiffuseSampler, uv + vec2(split, 0.0)).r;
    col.b = texture(DiffuseSampler, uv - vec2(split, 0.0)).b;
    // вспышка в цвете стихии — ровная по всему кадру
    col += Tint * Intensity * Intensity * 0.35;
    // saturate + harden
    float lum = dot(col, vec3(0.299, 0.587, 0.114));
    col = mix(vec3(lum), col, 1.0 + 0.7 * Intensity);
    col = pow(col, vec3(1.0 + 0.25 * Intensity));
    fragColor = vec4(col, 1.0);
}
