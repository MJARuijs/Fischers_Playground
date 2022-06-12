#version 300 es

layout (location = 0) in vec2 inPosition;

out vec2 textureCoordinates;

void main() {
    textureCoordinates = inPosition.xy;

    gl_Position = vec4(inPosition, 0, 1);
}
