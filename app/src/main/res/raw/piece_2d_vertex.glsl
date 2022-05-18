#version 300 es

layout (location = 0) in vec2 inPosition;

uniform vec2 translation;
uniform vec2 scale;
uniform float aspectRatio;

out vec2 textureCoordinates;

void main() {
    textureCoordinates = vec2(inPosition);

    vec2 position = (inPosition);
    position.y *= -1.0f;

    position *= scale;
    position += translation;
    position.x /= aspectRatio;

    gl_Position = vec4(position, 0, 1);

}
