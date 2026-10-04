#version 150

// the space tear: an expanding shock ring that rips the screen apart with
// radial distortion and an electric rim — used for the J blast, the F
// transformation, the V cast and meteor impacts
uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Time;      // seconds since the tear opened
uniform vec2 Center;     // uv space
uniform float Intensity; // 1 -> 0 as the tear closes

in vec2 texCoord;
out vec4 fragColor;

void main() {
    float aspect = OutSize.x / OutSize.y;
    vec2 uv = texCoord;
    vec2 d = uv - Center;
    d.x *= aspect;
    float dist = length(d);

    // the tear front races outward; behind it space keeps ringing
    float front = Time * 1.6;
    float behind = smoothstep(front, front - 0.35, dist);
    float ring = sin(dist * 60.0 - Time * 22.0);
    float tear = behind * Intensity;

    // radial displacement — the screen is pulled INTO the rift
    vec2 dir = dist > 0.0001 ? d / dist : vec2(0.0);
    vec2 offset = dir * ring * 0.035 * tear;
    offset.x /= aspect;
    vec3 col = texture(DiffuseSampler, uv - offset).rgb;

    // chromatic split right on the tear edge
    float edge = behind * smoothstep(0.35, 0.0, abs(dist - front + 0.18));
    col.r = texture(DiffuseSampler, uv - offset * 1.35).r;
    col.b = texture(DiffuseSampler, uv - offset * 0.65).b;

    // electric rim
    col += vec3(0.45, 0.75, 1.0) * edge * Intensity * (0.6 + 0.4 * ring);
    // the whole scene flashes blue-white at the start
    col += vec3(0.35, 0.55, 0.9) * max(0.0, 1.0 - Time * 3.0) * 0.45;

    fragColor = vec4(col, 1.0);
}
