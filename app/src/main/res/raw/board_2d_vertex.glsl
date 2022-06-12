#version 300 es

const int MAX_NUMBER_OF_SQUARES = 27;

layout (location = 0) in vec2 inPosition;

//uniform vec2 selectedSquareCoordinates;
//uniform vec2 checkedKingSquare;

//flat out int squareSelected;

out vec2 textureCoordinates;

void main() {
    textureCoordinates = inPosition.xy;

//    vec2 position = (inPosition.xy) ;

//    squareSelected = 0;

//    if (position.x == checkedKingSquare.x && position.y == checkedKingSquare.y) {
//        squareSelected = 1;
//    }
//
//    if (position.x == selectedSquareCoordinates.x && position.y == selectedSquareCoordinates.y) {
//        squareSelected = 2;
//    }

    gl_Position = vec4(inPosition, 0, 1);
}
