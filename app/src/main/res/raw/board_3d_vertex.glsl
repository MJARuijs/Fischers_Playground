#version 300 es

const int MAX_NUMBER_OF_SQUARES = 27;

layout (location = 0) in vec4 inPosition;

uniform vec2 selectedSquareCoordinates;
uniform vec2 checkedKingSquare;
//uniform vec2 possibleSquares[MAX_NUMBER_OF_SQUARES];

uniform mat4 projection;
uniform mat4 view;

flat out int squareSelected;

out float normalIndex;
out vec3 worldPosition;

void main() {
    vec3 position = inPosition.xyz;
    normalIndex = inPosition.w;

    squareSelected = 0;

    if (position.x == checkedKingSquare.x && position.y == checkedKingSquare.y) {
        squareSelected = 1;
    }

    if (position.x == selectedSquareCoordinates.x && position.y == selectedSquareCoordinates.y) {
        squareSelected = 2;
    }
//
//    for (int i = 0; i < MAX_NUMBER_OF_SQUARES; i++) {
//        vec2 possibleSquare = possibleSquares[i];
//
//        if (position.x == possibleSquare.x && position.y == possibleSquare.y) {
//            squareSelected = 2;
//        }
//    }

    worldPosition = inPosition.xyz;

    gl_Position = projection * view * vec4(worldPosition, 1);
}
