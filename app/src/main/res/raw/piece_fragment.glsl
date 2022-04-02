#version 300 es

in vec2 textureCoordinates;

uniform sampler2D textureMap;

out vec4 outColor;

void main() {

    vec4 textureColor = texture(textureMap, textureCoordinates);
//    if (textureColor.a <= 0.00) {
//        outColor = vec4(1, 0, 0, 1);
//    } else {
        outColor = textureColor;
//    }

    //    if (textureColor.a >= 0.0) {
    //        if (tileColor == 1.0) {
    //            gl_FragColor = vec4(whiteTile, 1.0);
    //        } else if (tileColor == 0.0) {
    //            gl_FragColor = vec4(darkTileTile, 1.0);
    //        }
    //    } else {
    //        gl_FragColor = textureColor;
    //    }

    //    gl_FragColor = vec4(1, 0, 0, 1);
}