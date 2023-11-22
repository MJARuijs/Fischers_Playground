#version 300 es

precision highp float;

in highp vec2 textureCoords;
in highp vec2 translation;
in highp float effect;
uniform highp sampler2DArray effectSampler;

out vec4 outColor;

void main() {
    if (translation.x < -1.0) {
        discard;
    }
    outColor = texture(effectSampler, vec3(textureCoords, int(effect)));
}