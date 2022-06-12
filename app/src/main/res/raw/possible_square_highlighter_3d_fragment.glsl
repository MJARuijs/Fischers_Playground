#version 300 es

precision highp float;

in highp vec2 translation;
in highp vec2 textureCoords;

uniform highp sampler2D circleTexture;

out vec4 outColor;

void main() {
    if (translation.x < -1.0) {
        discard;
    }

    vec4 textureColor = texture(circleTexture, textureCoords);
    outColor = vec4(0, 0.75, 0, textureColor.a);
}
