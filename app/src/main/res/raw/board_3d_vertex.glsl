#version 300 es

layout (location = 0) in vec4 inPosition;

uniform vec2 selectedSquareCoordinates;
uniform vec2 checkedKingSquare;

uniform mat4 projection;
uniform mat4 view;

//uniform float aspectRatio;

flat out int squareSelected;

out float normalIndex;
out vec3 worldPosition;

void main() {
    vec3 position = inPosition.xyz;
//    position.y /= aspectRatio;
    normalIndex = inPosition.w;

    squareSelected = 0;

    if (position.x == checkedKingSquare.x && position.y == checkedKingSquare.y) {
        squareSelected = 1;
    }

    if (position.x == selectedSquareCoordinates.x && position.y == selectedSquareCoordinates.y) {
        squareSelected = 2;
    }

    worldPosition = inPosition.xyz;
//    worldPosition.y /= 0.5;
    vec4 pos = projection * view * vec4(worldPosition, 1);
//    pos.y *= aspectRatio;
    gl_Position = pos;
}
