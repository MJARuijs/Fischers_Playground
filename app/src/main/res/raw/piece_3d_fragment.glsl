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

uniform AmbientLight ambientLight;
uniform DirectionalLight sun;
uniform Material material;
uniform vec3 cameraPosition;

out vec4 outColor;

vec4 computeAmbientColor() {
    return ambientLight.color * material.ambient;
//    return vec4(1, 1, 1, 1) * ambientLight.color;
}

vec4 computeDirectionalColor(vec3 lightDirection) {

    // Diffuse
    vec3 normalDirection = normalize(passNormal);
    lightDirection = normalize(lightDirection);

    float brightness = clamp(dot(lightDirection, normalDirection), 0.0, 1.0);

    vec4 diffuse = material.diffuse * sun.color * brightness;

    // Specular
    vec3 position = worldPosition.xyz;
    vec3 reflectionVector = 2.0 * (dot(lightDirection, normalDirection)) * normalDirection - lightDirection;
    vec3 toCameraVector = normalize(cameraPosition - position);

    vec4 specular = material.specular * sun.color * clamp(pow(dot(reflectionVector, toCameraVector), material.shininess), 0.0, 1.0);

    return diffuse + specular;
//    return vec4(passNormal, 1);
}

void main() {
    if (passNormal.x == 0.0 && passNormal.y == 0.0 && passNormal.z == 0.0) {
//        discard;
    }
    vec4 ambientColor = computeAmbientColor();
    vec4 sunColor = computeDirectionalColor(sun.direction);
//
    outColor = ambientColor + sunColor;
//    outColor = vec4(abs(passNormal), 1);
//    ;
//    outColor = vec4(1, 0, 0, 1);
}
