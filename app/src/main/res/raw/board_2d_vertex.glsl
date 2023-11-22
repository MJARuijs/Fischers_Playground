#version 300 es

layout (location = 0) in vec2 inPosition;

//uniform float aspectRatio;

out vec2 textureCoordinates;

void main() {
    textureCoordinates = inPosition.xy;

    vec2 position = inPosition;
//    position.x /= aspectRatio;
//    position /= 2.0;
    position.y += 0.00001;

    gl_Position = vec4(position, 0, 1);
}
