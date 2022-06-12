#version 300 es

layout (location = 0) in vec2 inPosition;

uniform vec2 translations[30];

out vec2 translation;

void main() {
    vec2 position = inPosition * 2.0 - 1.0;

    translation = translations[gl_InstanceID];
    position *= vec2(1.0 / 8.0, 1.0 / 8.0);
    position += translation;

    gl_Position = vec4(position + vec2(1.0, 1.0) / 8.0, 0.01, 1);
}
