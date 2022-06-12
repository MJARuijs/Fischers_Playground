#version 300 es

layout (location = 0) in vec2 inPosition;

uniform vec2 translations[30];
uniform mat4 projection;
uniform mat4 view;

out vec2 translation;
out vec2 textureCoords;

void main() {
    textureCoords = vec2(inPosition);
    vec2 squareOffset = vec2(1.0 / 8.0, 1.0 / 8.0);

    vec2 position = (inPosition * 2.0 - 1.0) / 8.0;
    position /= 2.0f;
    position += squareOffset;

    translation = translations[gl_InstanceID];
    position += translation;

    gl_Position = projection * view * vec4(position, 0.005, 1);
}
