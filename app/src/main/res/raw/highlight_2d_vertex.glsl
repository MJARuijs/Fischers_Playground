#version 300 es

layout (location = 0) in vec2 inPosition;

uniform vec2 translation;
uniform vec2 scale;
uniform float aspectRatio;

//out float passAspectRatio;
out vec2 passPosition;

void main() {
    vec2 position = (inPosition)  * 2.0 - 1.0;

    position *= scale;
    position += translation;
    position.x /= aspectRatio;
//    + vec2(aspectRatio, aspectRatio) / 4.0

    passPosition = position ;

//    position += vec2(aspectRatio, aspectRatio) / 8.0;
    gl_Position = vec4(position+ vec2(aspectRatio, aspectRatio) / 8.0, 0, 1);
}
