#version 300 es

layout (location = 0) in vec3 inPosition;

uniform vec2 scale;
uniform float aspectRatio;
uniform vec2 selectedSquareCoordinates;

flat out int squareSelected;
out vec2 textureCoordinates;
out float tileColor;

void main() {
    textureCoordinates = inPosition.xy;
    tileColor = inPosition.z;

    vec2 position = inPosition.xy;

    squareSelected = 0;
    if (position.x == selectedSquareCoordinates.x && position.y == selectedSquareCoordinates.y) {
        squareSelected = 1;
    }

    position.x /= aspectRatio;
    position *= scale;


    gl_Position = vec4(position, 0, 1);
}
