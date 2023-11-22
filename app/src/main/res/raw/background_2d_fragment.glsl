#version 300 es

precision highp float;

in vec2 textureCoordinates;

uniform highp sampler2D textureMap;

out vec4 outColor;

void main() {

    vec2 position = textureCoordinates;
//    vec2 position = (textureCoordinates + vec2(1, 1)) / 2.0;

    vec4 textureColor = texture(textureMap, position);
    outColor = textureColor;
//    outColor = vec4(1,0,0,1);
}
