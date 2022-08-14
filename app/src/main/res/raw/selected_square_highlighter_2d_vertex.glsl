#version 300 es

const vec2 SQUARE_SIZE = vec2(1.0 / 8.0, 1.0 / 8.0);

layout (location = 0) in vec2 inPosition;

uniform vec2 translations[2];
uniform vec4 colors[2];

uniform float aspectRatio;

out vec2 translation;
out vec4 color;

void main() {
    translation = translations[gl_InstanceID];
    translation /= vec2(aspectRatio, 1.0);
    translation.x /= 2.0;

    color = colors[gl_InstanceID];

    vec2 position = inPosition;
    position *= SQUARE_SIZE;
    position.x /= aspectRatio;
    position += (translation / vec2(1.0, 2.0));

    gl_Position = vec4(position, 0, 1);
}
