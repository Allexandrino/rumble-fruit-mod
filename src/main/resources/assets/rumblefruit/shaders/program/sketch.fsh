#version 150
// pencil-sketch impact frame: the world is redrawn as a hand-drawn sketch —
// sobel edges as pencil strokes, warm paper base, hatching in the shadows,
// and a hand-jitter wobble so the frame feels drawn, not computed
uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Intensity;
uniform float Time;
in vec2 texCoord;
out vec4 fragColor;

float lumAt(vec2 uv) {
    return dot(texture(DiffuseSampler, uv).rgb, vec3(0.299, 0.587, 0.114));
}

void main() {
    vec2 px = 1.0 / OutSize;
    // the hand never holds still: jitter the sample grid a hair
    vec2 juv = texCoord + vec2(sin(Time * 43.0 + texCoord.y * 130.0),
                               cos(Time * 37.0 + texCoord.x * 130.0)) * px * 1.2;
    float tl = lumAt(juv + px * vec2(-1.0, -1.0));
    float l  = lumAt(juv + px * vec2(-1.0,  0.0));
    float bl = lumAt(juv + px * vec2(-1.0,  1.0));
    float t  = lumAt(juv + px * vec2( 0.0, -1.0));
    float b  = lumAt(juv + px * vec2( 0.0,  1.0));
    float tr = lumAt(juv + px * vec2( 1.0, -1.0));
    float r  = lumAt(juv + px * vec2( 1.0,  0.0));
    float br = lumAt(juv + px * vec2( 1.0,  1.0));
    float gx = -tl - 2.0 * l - bl + tr + 2.0 * r + br;
    float gy = -tl - 2.0 * t - tr + bl + 2.0 * b + br;
    float edge = clamp(length(vec2(gx, gy)) * 2.2, 0.0, 1.0);

    float lum = lumAt(juv);
    // warm paper, dimmed where the scene is dark
    float paper = 0.94 - 0.30 * (1.0 - lum);
    // cross-hatching in the shadows
    float hatch = 0.0;
    if (lum < 0.6) {
        float h1 = step(0.55, fract((juv.x + juv.y) * OutSize.y * 0.30));
        float h2 = step(0.65, fract((juv.x - juv.y) * OutSize.y * 0.30));
        hatch = (h1 * 0.6 + h2 * 0.4) * (0.6 - lum) * 1.2;
    }
    float pencil = clamp(paper - edge * 0.9 - hatch, 0.0, 1.0);
    vec3 sketch = vec3(pencil) * vec3(0.98, 0.96, 0.90);
    vec3 scene = texture(DiffuseSampler, texCoord).rgb;
    fragColor = vec4(mix(scene, sketch, Intensity), 1.0);
}
