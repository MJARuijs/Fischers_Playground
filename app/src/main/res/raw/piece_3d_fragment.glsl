#version 300 es

precision highp float;

struct AmbientLight {
    vec4 color;
};

struct DirectionalLight {
    vec4 color;
    vec3 direction;
};

struct Material {
    vec4 ambient;
    vec4 diffuse;
    vec4 specular;
    float shininess;
};

in vec4 worldPosition;
in vec3 passNormal;
in vec2 passTextureCoordinates;

uniform highp sampler2DArray textureMaps;
uniform float textureId;
uniform float isWhite;

uniform float rChannel;
uniform float gChannel;
uniform float bChannel;

uniform AmbientLight ambientLight;
uniform DirectionalLight sun;
uniform Material material;
uniform vec3 cameraPosition;

out vec4 outColor;

vec4 computeAmbientColor(vec4 textureColor) {
    if (textureId == -1.0) {
        return ambientLight.color * material.ambient;
    } else {
        return ambientLight.color * textureColor;
    }
}

vec4 computeDirectionalColor(vec3 lightDirection, vec4 textureColor) {

    // Diffuse
    vec3 normalDirection = normalize(passNormal);
    lightDirection = normalize(lightDirection);

    float brightness = clamp(dot(lightDirection, normalDirection), 0.0, 1.0);

    vec4 diffuse;
    if (textureId == -1.0) {
        diffuse = material.diffuse * sun.color * brightness;
    } else {
        diffuse = textureColor * sun.color * brightness;
    }

    // Specular
    vec3 position = worldPosition.xyz;
    vec3 reflectionVector = 2.0 * (dot(lightDirection, normalDirection)) * normalDirection - lightDirection;
    vec3 toCameraVector = normalize(cameraPosition - position);

    vec4 specular;
    if (textureId == -1.0) {
        specular = material.specular * sun.color * clamp(pow(dot(reflectionVector, toCameraVector), material.shininess), 0.0, 1.0);
    } else {
        specular = textureColor * sun.color * clamp(pow(dot(reflectionVector, toCameraVector), material.shininess), 0.0, 1.0);
    }

    return diffuse + specular;
}

void main() {
    vec4 textureColor = texture(textureMaps, vec3(passTextureCoordinates, int(textureId)));
    if (isWhite == 1.0) {
        vec3 darkColor = textureColor.rgb;
        float strength = (darkColor.r + darkColor.g + darkColor.b) / 3.0;
//        textureColor.rgb = vec3(238.0 / 255.0, 255.0 / 255.0, 190.0 / 255.0) - vec3(1.0, 1.0, 1.0) * strength;
//        textureColor.rgb = vec3(rChannel, gChannel, bChannel) - vec3(1.0, 1.0, 1.0) * strength;
        textureColor.rgb = vec3(1.0, 0.9608, 0.8980) - vec3(1.0, 1.0, 1.0) * strength;

        // r=1.0, g=0.9607843, b=0.8980392

//        textureColor.rgb = vec3(1.0, 0.9, 0.6) - (vec3(1.0, 1.0, 1.0) * darkColor.r);
    } else {
        vec3 darkColor = textureColor.rgb;
//        textureColor.rgb = vec3(rChannel, gChannel, bChannel) * darkColor.r ;
        textureColor.rgb = vec3(1.0, 0.8196, 0.4196) * darkColor.r ;
    }

    vec4 ambientColor = computeAmbientColor(textureColor);
    vec4 sunColor = computeDirectionalColor(sun.direction, textureColor);
//
//    outColor = textureColor;
//    if (textureColor.r == 0.0) {
//        outColor = vec4(1,0,0,1);
//        outColor = ambientColor + sunColor;

//    } else {
//        outColor = vec4(0, 1, 0, 1);
        outColor = ambientColor + sunColor;

//    }
//    outColor = ambientColor + sunColor;

}
