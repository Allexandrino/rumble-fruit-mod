#version 150
in vec4 Position;
uniform vec2 OutSize;
out vec2 texCoord;
void main() {
    // the fullscreen quad arrives in pixel coords; the framework does not set
    // ProjMat here, so compute the clip position directly
    texCoord = Position.xy / OutSize;
    gl_Position = vec4(texCoord * 2.0 - 1.0, 0.0, 1.0);
}
