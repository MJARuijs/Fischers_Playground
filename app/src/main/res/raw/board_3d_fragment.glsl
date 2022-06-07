#version 300 es

precision highp float;

const vec3 whiteTile = vec3(196.0 / 255.0, 178.0 / 255.0, 158.0 / 255.0);
const vec3 darkTile = vec3(109.0 / 255.0, 86.0 / 255.0, 68.0 / 255.0);

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

in float normalIndex;

flat in int squareSelected;
in vec3 worldPosition;

uniform highp sampler2D diffuseTexture;
uniform highp sampler2D normalTexture;
uniform highp sampler2D specularTexture;

uniform AmbientLight ambientLight;
uniform DirectionalLight sun;
uniform Material material;
uniform vec3 cameraPosition;

out vec4 outColor;

vec4 computeAmbientColor() {
    return ambientLight.color * material.ambient;
}

vec4 computeAmbientColor(vec4 color) {
    return ambientLight.color * color;
}

vec4 computeDirectionalColor(vec3 lightDirection, vec3 normal) {

    // Diffuse
    vec3 normalDirection = normalize(normal);
    lightDirection = normalize(lightDirection);

    float brightness = clamp(dot(lightDirection, normalDirection), 0.0, 1.0);

    vec4 diffuse = material.diffuse * sun.color;
    diffuse.rgb *= brightness;

    // Specular
    vec3 reflectionVector = 2.0 * (dot(lightDirection, normalDirection)) * normalDirection - lightDirection;
    vec3 toCameraVector = normalize(cameraPosition - worldPosition);

    vec4 specular = material.specular * sun.color * clamp(pow(dot(reflectionVector, toCameraVector), material.shininess), 0.0, 1.0);

    return diffuse + specular;
}

vec4 computeDirectionalColor(vec3 lightDirection, vec4 color, vec3 normal, float specularStrength) {

    // Diffuse
    vec3 normalDirection = normalize(normal);
    lightDirection = normalize(lightDirection);

    float brightness = clamp(dot(lightDirection, normalDirection), 0.0, 1.0);

    vec4 diffuse = color * sun.color;
    diffuse.rgb *= brightness;

    // Specular
    vec3 reflectionVector = 2.0 * (dot(lightDirection, normalDirection)) * normalDirection - lightDirection;
    vec3 toCameraVector = normalize(cameraPosition - worldPosition);

//    float specularFactor
//    float dampedFactor = pow(spe)

//    vec4 specular = sun.color;
//    specular.rgb *=
    vec4 specular = sun.color * clamp(pow(dot(reflectionVector, toCameraVector), specularStrength), 0.0, 1.0);

    return diffuse;
}

void main() {
    if (normalIndex == 0.0f) {
        vec2 position = (worldPosition.xy + vec2(1, 1)) / 2.0;

        vec4 textureColor = texture(diffuseTexture, position);
        vec3 normal = normalize(texture(normalTexture, position).xyz);
        float specularValue = texture(specularTexture, position).r;
        float strength = (textureColor.r + textureColor.g + textureColor.b) / 2.0;

        float scale = 1.0 / 8.0;
        int squareX = 0;
        for (int x = 1; x < 9; x++) {
            if (position.x < float(x) * scale) {
                squareX = x - 1;
                break;
            }
        }

        int squareY = 0;
        for (int y = 1; y < 9; y++) {
            if (position.y < float(y) * scale) {
                squareY = y - 1;
                break;
            }
        }

        int remainderX = squareX % 2;
        int remainderY = squareY % 2;

        vec4 squareColor = vec4(0, 0, 0, 1);

        if (remainderY == 0) {
            if (remainderX == 0) {
                squareColor.rgb = darkTile * strength;
            } else {
                squareColor.rgb = whiteTile * strength;
            }
        } else {
            if (remainderX == 1) {
                squareColor.rgb = darkTile * strength;
            } else {
                squareColor.rgb = whiteTile * strength;
            }
        }

//        squareColor.rgb *= 0.8;

        outColor = squareColor;

//        vec4 ambientColor = computeAmbientColor(squareColor);
//        vec4 sunColor = computeDirectionalColor(sun.direction, squareColor, normal, specularValue * 5.0);

//        outColor = ambientColor + sunColor;

//        outColor = textureColor ;
//        outColor.rgb = normal;

//        outColor = textureColor;
//        outColor.a = 1.0;
    } else {
        vec3 normal;

        if (normalIndex == 1.0) {
            normal = vec3(1, 0, 0);
        } else if (normalIndex == 2.0) {
            normal = vec3(-1, 0, 0);
        } else if (normalIndex == 3.0) {
            normal = vec3(0, 1, 0);
        } else if (normalIndex == 4.0) {
            normal = vec3(0, -1, 0);
        } else if (normalIndex == 5.0) {
            normal = vec3(0, 0, 1);
        } else if (normalIndex == 6.0) {
            normal = vec3(0, 0, -1);
        }

        vec4 ambientColor = computeAmbientColor();
        vec4 sunColor = computeDirectionalColor(sun.direction, normal);

        outColor = ambientColor + sunColor;
    }


//    if (tileColor == 1.0) {
//        outColor = vec4(whiteTile, 1.0);
//    } else if (tileColor == 0.0) {
//        outColor = vec4(darkTile, 1.0);
//    }
//
//    if (squareSelected == 1) {
//        outColor = vec4(0, 0, 1, 1);
//    } else if (squareSelected == 2) {
//        outColor = vec4(0.5, 0, 0, 1);
//    } else if (squareSelected == 3) {
//        outColor = vec4(0.5, 0, 0, 1);
//    }
}