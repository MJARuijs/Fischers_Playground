#version 300 es

layout (location = 0) in vec2 inPosition;

uniform vec2 translation;
uniform vec2 scale;

uniform float aspectRatio;

out vec2 textureCoordinates;

void main() {
    textureCoordinates = vec2(inPosition);

    vec2 position = inPosition;
    position *= scale;

    position.x /= aspectRatio;
    position.y *= -1.0f;

    position /= 2.0;
    position += (translation / vec2(1, 2.0));

    gl_Position = vec4(position, 0, 1);
}
