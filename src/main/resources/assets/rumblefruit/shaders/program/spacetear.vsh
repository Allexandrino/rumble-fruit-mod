#version 150

in vec4 Position;

uniform mat4 ProjMat;
uniform vec2 InSize;
uniform vec2 OutSize;

out vec2 texCoord;

void main() {
    gl_Position = ProjMat * Position;
    texCoord = Position.xy / OutSize;
}
