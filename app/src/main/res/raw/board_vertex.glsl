#version 300 es

layout (location = 0) in vec3 inPosition;

uniform vec2 translation;
uniform vec2 scale;
uniform float aspectRatio;

out vec2 textureCoordinates;
out float tileColor;

void main() {
    textureCoordinates = inPosition.xy;
    tileColor = inPosition.z;

    vec2 position = inPosition.xy;
    position.x /= aspectRatio;
    position *= scale;
    position += translation;

    gl_Position = vec4(position, 0, 1);
}
