#version 300 es

const int MAX_NUMBER_OF_SQUARES = 27;

layout (location = 0) in vec4 inPosition;

//uniform vec2 scale;
//uniform float aspectRatio;
uniform vec2 selectedSquareCoordinates;
uniform vec2 checkedKingSquare;
uniform vec2 possibleSquares[MAX_NUMBER_OF_SQUARES];

uniform mat4 projection;
uniform mat4 view;

flat out int squareSelected;

out vec2 textureCoordinates;
out float tileColor;
out vec3 worldPosition;

void main() {
    textureCoordinates = inPosition.xy;
    tileColor = inPosition.w;

    vec3 position = inPosition.xyz;

    squareSelected = 0;

    if (position.x == checkedKingSquare.x && position.y == checkedKingSquare.y) {
        squareSelected = 3;
    }

    if (position.x == selectedSquareCoordinates.x && position.y == selectedSquareCoordinates.y) {
        squareSelected = 1;
    }

    for (int i = 0; i < MAX_NUMBER_OF_SQUARES; i++) {
        vec2 possibleSquare = possibleSquares[i];

        if (position.x == possibleSquare.x && position.y == possibleSquare.y) {
            squareSelected = 2;
        }
    }

//    position.x /= aspectRatio;
//    position *= scale;

    worldPosition = inPosition.xyz;

    gl_Position = projection * view * vec4(worldPosition, 1);
}