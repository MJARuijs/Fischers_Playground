#version 300 es

precision highp float;

const vec2 SQUARE_SIZE = vec2(1.0 / 8.0, 1.0 / 8.0);

in highp vec2 translation;
in highp vec4 color;

uniform highp vec2 viewPort;

out vec4 outColor;

void main() {
    vec2 center = translation * 8.0 + vec2(1.0, 1.0);

    vec2 currentPoint = gl_FragCoord.xy;
    currentPoint /= viewPort;
    currentPoint = (currentPoint * 2.0) - 1.0;
    currentPoint /= SQUARE_SIZE;

    float maxDistance = distance(currentPoint, center) / 1.5;

    outColor = mix(vec4(1,1,1,1), color, maxDistance);
}