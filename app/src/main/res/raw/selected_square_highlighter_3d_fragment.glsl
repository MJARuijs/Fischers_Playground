#version 300 es

precision highp float;

in highp vec2 textureCoords;
in highp vec2 translation;
//in highp vec4 color;
in highp float effect;
uniform highp float aspectRatio;
uniform highp vec2 scale;
uniform highp vec2 viewPort;
uniform highp sampler2DArray effectSampler;
//uniform highp sampler2D kingCheckedTexture;
//uniform highp sampler2D squareSelectedTexture;

out vec4 outColor;

void main() {
        if (translation.x < -1.0) {
                discard;
        }
//    vec2 center = translation * 8.0 + vec2(aspectRatio, aspectRatio);
//
//    vec2 currentPoint = gl_FragCoord.xy;
//    currentPoint /= viewPort;
//    currentPoint = (currentPoint * 2.0) - 1.0;
//    currentPoint.x /= aspectRatio;
//    currentPoint /= scale;
//
//    float maxDistance = distance(currentPoint, center) / 1.5;

//    if (effect == 1.0) {
        outColor = texture(effectSampler, vec3(textureCoords, int(effect)));
//    } else if (effect == 2.0) {
//        outColor = texture(kingCheckedTexture, textureCoords);
//    }

//    outColor = texture(squareSelectedTexture, textureCoords);

//    outColor = mix(vec4(1,1,1,1), color, maxDistance);
//    outColor = color;
}