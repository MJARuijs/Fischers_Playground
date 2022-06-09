#version 300 es

precision highp float;

in highp vec2 translation;

uniform highp float aspectRatio;
uniform highp vec2 scale;

uniform highp vec2 viewPort;

out vec4 outColor;

void main() {

    float radius = 0.4;
    vec2 center = translation * 8.0 + vec2(aspectRatio, aspectRatio);

    vec2 currentPoint = gl_FragCoord.xy;
    currentPoint /= viewPort;
    currentPoint = (currentPoint * 2.0) - 1.0;
    currentPoint.x /= aspectRatio;
    currentPoint /= scale;

    if (distance(currentPoint, center) > radius) {
        outColor = vec4(0, 1, 0, 1);
        discard;
    } else {
        outColor = vec4(0, 0.75, 0, 1);
    }

}
