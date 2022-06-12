#version 300 es

precision highp float;

in highp vec2 translation;
in highp vec2 textureCoords;

uniform highp float zoom;
uniform highp vec2 viewPort;
uniform highp sampler2D circleTexture;

out vec4 outColor;

void main() {

    if (translation.x < -1.0) {
        discard;
    }

//    float radius = 1.0;
//    vec2 center = translation;

//    vec2 currentPoint = gl_FragCoord.xy;
//    currentPoint /= (viewPort ) ;
//    currentPoint = (currentPoint * 2.0) - 1.0;
////    currentPoint *= 8.0;
//
//    if (distance(currentPoint, center) > radius) {
//        outColor = vec4(1, 0, 0, 1);
////        discard;
//    } else {
//        outColor = vec4(0, 0.75, 0, 1);
//    }

    vec4 textureColor = texture(circleTexture, textureCoords);
    outColor = vec4(0, 0.75, 0, textureColor.a);
//    outColor = vec4(textureCoords, 0, 1);
}
