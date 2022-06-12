#version 300 es

const vec2 SQUARE_SIZE = vec2(1.0 / 8.0, 1.0 / 8.0);

layout (location = 0) in vec2 inPosition;

uniform vec2 translations[2];
uniform float effects[2];

uniform mat4 projection;
uniform mat4 view;

out vec2 textureCoords;
out vec2 translation;
out float effect;

void main() {
    vec2 position = inPosition * 2.0 - 1.0;
    textureCoords = inPosition;

    translation = translations[gl_InstanceID];
    effect = effects[gl_InstanceID];
    position *= SQUARE_SIZE;
    position += translation;

    gl_Position = projection * view * vec4(position + SQUARE_SIZE, 0.005, 1);
}
