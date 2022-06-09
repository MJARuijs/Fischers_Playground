#version 300 es
precision highp float;

in vec2 passPosition;
//in float passAspectRatio;
uniform highp float aspectRatio;
uniform highp vec2 translation;
uniform highp vec2 scale;

uniform highp vec2 viewPort;

out vec4 outColor;

void main() {

    float radius = 0.6;
    vec2 center = translation + vec2(aspectRatio, aspectRatio);
//    + vec2(aspectRatio, aspectRatio) / 2.0
    vec2 currentPoint = gl_FragCoord.xy;
    currentPoint /= viewPort;
    currentPoint = (currentPoint * 2.0) - 1.0;
    currentPoint.x /= aspectRatio;
    currentPoint /= scale;

    if (distance(currentPoint, center) > radius) {
//        outColor = vec4(0, 1, 0, 1);
        discard;
    } else {
        outColor = vec4(1, 0, 0, 1);
    }

}
