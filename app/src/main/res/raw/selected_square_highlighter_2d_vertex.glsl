#version 300 es

const vec2 SQUARE_SIZE = vec2(1.0 / 8.0, 1.0 / 8.0);

layout (location = 0) in vec2 inPosition;

uniform vec2 translations[2];
uniform vec4 colors[2];

out vec2 translation;
out vec4 color;

void main() {
    vec2 position = inPosition * 2.0 - 1.0;

    translation = translations[gl_InstanceID];
    color = colors[gl_InstanceID];
    position *= SQUARE_SIZE;
    position += translation;

    gl_Position = vec4(position + SQUARE_SIZE, 0, 1);
}
