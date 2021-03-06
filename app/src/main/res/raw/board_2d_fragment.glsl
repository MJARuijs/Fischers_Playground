#version 300 es

precision highp float;

const vec3 whiteTile = vec3(196.0 / 255.0, 178.0 / 255.0, 158.0 / 255.0);
const vec3 darkTile = vec3(109.0 / 255.0, 86.0 / 255.0, 68.0 / 255.0);

in vec2 textureCoordinates;

uniform highp sampler2D textureMap;

out vec4 outColor;

void main() {
    vec2 position = (textureCoordinates + vec2(1, 1)) / 2.0;

    vec4 textureColor = texture(textureMap, position * vec2(1, -1));
    float strength = (textureColor.r + textureColor.g + textureColor.b) / 2.0;

    float scale = 1.0 / 8.0;

    int squareX = 0;
    for (int x = 1; x < 9; x++) {
        if (position.x < float(x) * scale) {
            squareX = x - 1;
            break;
        }
    }

    int squareY = 0;
    for (int y = 1; y < 9; y++) {
        if (position.y < float(y) * scale) {
            squareY = y - 1;
            break;
        }
    }

    int remainderX = squareX % 2;
    int remainderY = squareY % 2;

    outColor.a = 1.0;
    if (remainderY == 0) {
        if (remainderX == 0) {
            outColor.rgb = darkTile * strength;
        } else {
            outColor.rgb = whiteTile * strength;
        }
    } else {
        if (remainderX == 1) {
            outColor.rgb = darkTile * strength;
        } else {
            outColor.rgb = whiteTile * strength;
        }
    }
}