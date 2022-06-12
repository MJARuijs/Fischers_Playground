#version 300 es

layout (location = 0) in vec2 inPosition;

uniform vec2 translations[2];
uniform float effects[2];
uniform vec2 scale;
uniform float aspectRatio;

uniform mat4 projection;
uniform mat4 view;

out vec2 textureCoords;
out vec2 translation;
out float effect;
//out vec2 passPosition;

void main() {
    vec2 position = inPosition * 2.0 - 1.0;
    textureCoords = inPosition;

    translation = translations[gl_InstanceID];
    effect = effects[gl_InstanceID];
//    color = colors[gl_InstanceID];
    position *= scale;
    position += translation;

    //    passPosition = position;

    gl_Position = projection * view * vec4(position + vec2(aspectRatio, aspectRatio) / 8.0, 0.005, 1);
}
