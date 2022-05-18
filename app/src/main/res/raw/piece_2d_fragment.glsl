#version 300 es

precision highp float;

in vec2 textureCoordinates;

uniform highp sampler2DArray textureMaps;
uniform float textureId;

out vec4 outColor;

void main() {
    vec4 textureColor = texture(textureMaps, vec3(textureCoordinates, int(textureId)));
    outColor = textureColor;
}