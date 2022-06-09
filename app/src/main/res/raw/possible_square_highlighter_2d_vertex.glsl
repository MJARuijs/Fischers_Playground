#version 300 es

layout (location = 0) in vec2 inPosition;

uniform vec2 translations[30];
uniform vec2 scale;
uniform float aspectRatio;

out vec2 translation;
//out vec2 passPosition;

void main() {
    vec2 position = inPosition * 2.0 - 1.0;

    translation = translations[gl_InstanceID];
    position *= scale;
    position += translation;

//    passPosition = position;

    gl_Position = vec4(position + vec2(aspectRatio, aspectRatio) / 8.0, 0, 1);
}
