#version 300 es

precision highp float;

in vec2 textureCoordinates;

uniform highp sampler2DArray textureMaps;
uniform float textureId;
uniform float alpha;

out vec4 outColor;

void main() {
    vec4 textureColor = texture(textureMaps, vec3(textureCoordinates, int(textureId)));
    // if (textureColor.a == 0.0) {
    //    outColor = vec4(1, 0, 0, 1);
    //} else {
        outColor = textureColor;
    //}
    if (outColor.a > 0.5) {
        outColor.a = alpha;
    }
}