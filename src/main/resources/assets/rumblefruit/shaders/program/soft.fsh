#version 150
// мягкая картинка древнего мира: лёгкое сглаживание 3x3 (8%) и тёплый
// тон — текстуры перестают «звенеть» пикселями на дальних дистанциях
uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec2 uv = texCoord;
    vec2 px = 1.0 / OutSize;
    vec3 col = texture(DiffuseSampler, uv).rgb;
    vec3 soft = col * 4.0;
    soft += texture(DiffuseSampler, uv + vec2(px.x, 0.0)).rgb * 2.0;
    soft += texture(DiffuseSampler, uv - vec2(px.x, 0.0)).rgb * 2.0;
    soft += texture(DiffuseSampler, uv + vec2(0.0, px.y)).rgb * 2.0;
    soft += texture(DiffuseSampler, uv - vec2(0.0, px.y)).rgb * 2.0;
    soft += texture(DiffuseSampler, uv + px).rgb;
    soft += texture(DiffuseSampler, uv - px).rgb;
    soft += texture(DiffuseSampler, uv + vec2(px.x, -px.y)).rgb;
    soft += texture(DiffuseSampler, uv + vec2(-px.x, px.y)).rgb;
    soft /= 16.0;
    col = mix(col, soft, 0.55);
    // тёплый средиземноморский тон + лёгкая насыщенность
    float lum = dot(col, vec3(0.299, 0.587, 0.114));
    col = mix(vec3(lum), col, 1.12);
    col *= vec3(1.04, 1.0, 0.96);
    fragColor = vec4(col, 1.0);
}
