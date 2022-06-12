#version 300 es

precision highp float;

const vec3 whiteTile = vec3(196.0 / 255.0, 178.0 / 255.0, 158.0 / 255.0);
const vec3 darkTile = vec3(109.0 / 255.0, 86.0 / 255.0, 68.0 / 255.0);

const int KING_CHECK_INDEX = 1;
const int SQUARE_SELECTED_INDEX = 2;

in vec2 textureCoordinates;

flat in int squareSelected;

uniform highp sampler2D textureMap;
uniform highp vec2 selectedSquareCoordinates;
uniform highp vec2 checkedKingSquare;
uniform highp vec2 viewPort;


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

    if (squareSelected != 0) {
        vec2 center;
        vec4 color;

        if (squareSelected == KING_CHECK_INDEX) {
            center = checkedKingSquare * 8.0 + vec2(1.0, 1.0);
            color = vec4(1, 0, 0, 1);
        } else if (squareSelected == SQUARE_SELECTED_INDEX) {
            center = selectedSquareCoordinates * 8.0 + vec2(1.0, 1.0) ;
            color = vec4(0, 0, 1, 1);
        }

        vec2 currentPoint = gl_FragCoord.xy;
        currentPoint /= viewPort;
        currentPoint = (currentPoint * 2.0) - 1.0;
        currentPoint /= vec2(1.0 / 8.0, 1.0 / 8.0);

        float maxDistance = distance(currentPoint, center) / 1.5;
        outColor = mix(vec4(1,1,1,1), color, maxDistance);
    }

//    vec2 center;
//    vec4 color;

//    if (squareSelected == KING_CHECK_INDEX) {
//        center = checkedKingSquare * 8.0 + vec2(1.0, 1.0);
//        color = vec4(1, 0, 0, 1);
//    } else if (squareSelected == SQUARE_SELECTED_INDEX) {
//        center = selectedSquareCoordinates * 8.0 + vec2(1.0, 1.0) ;

//    center = vec2(0, 0);
//        color = vec4(1, 0, 0, 1);
//    }

//    vec2 currentPoint = gl_FragCoord.xy;
//    currentPoint /= viewPort;
//    currentPoint = (currentPoint * 2.0) - 1.0;
//    currentPoint /= vec2(1.0 / 8.0, 1.0 / 8.0);
//
//    float maxDistance = (distance(currentPoint, center) / 1.5) / 8.0;
//    outColor = mix(vec4(1,1,1,1), color, maxDistance);
}