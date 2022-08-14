#version 300 es

precision highp float;

in highp vec2 translation;

uniform highp vec2 viewPort;

out vec4 outColor;

void main() {

    float radius = 0.3;
    float ringSize = 0.1;
    vec2 center = translation * 8.0 + vec2(1.0, 1.0);

    vec2 currentPoint = gl_FragCoord.xy;
    currentPoint /= viewPort;
    currentPoint = (currentPoint * 2.0) - 1.0;
    currentPoint /= vec2(1.0 / 8.0, 1.0 / 8.0);
//    currentPoint.y *= 2.0;

    float distance = distance(currentPoint, center);

    if (distance > radius + ringSize) {
        outColor = vec4(1, 0, 0, 1);
        discard;
//    } else if (distance > radius) {
//        outColor = vec4(1, 1, 1, 1);
    } else {
        outColor = vec4(0, 0.75, 0, 1);
    }

}
