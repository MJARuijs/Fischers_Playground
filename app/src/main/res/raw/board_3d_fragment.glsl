#version 300 es

precision highp float;

const vec3 whiteTile = vec3(0.75f, 0.75f, 0.75f);
const vec3 darkTile = vec3(0.25f, 0.25f, 0.25f);

in vec2 textureCoordinates;
in float tileColor;

flat in int squareSelected;
in vec3 worldPosition;
//uniform sampler2D textureMap;

out vec4 outColor;

void main() {
    if (tileColor == 1.0) {
        outColor = vec4(whiteTile, 1.0);
    } else if (tileColor == 0.0) {
        outColor = vec4(darkTile, 1.0);
    }

    if (squareSelected == 1) {
        outColor = vec4(0, 0, 1, 1);
    } else if (squareSelected == 2) {
        outColor = vec4(0.5, 0, 0, 1);
    } else if (squareSelected == 3) {
        outColor = vec4(0.5, 0, 0, 1);
    }
}