#version 300 es

precision mediump float;

in float varyingTexIndex;
in vec3 transfNormal;
in vec3 fragModel;

uniform sampler2D tex;
uniform vec3 lightPos;
uniform vec3 eyePos;

out vec4 fragColor;

void main() {
    vec2 texCoord = vec2(varyingTexIndex, 0);

    vec4 texColor = texture(tex, texCoord);
    vec4 diffuseComponent = texColor;
    vec4 specComponent = texColor;
    vec4 ambientComponent = 0.15 * texColor;

    vec3 eyeDir = normalize(eyePos-fragModel);
    vec3 lightDir = normalize(lightPos-fragModel);
    float diff = max(dot(lightDir,transfNormal),0.0);
    vec3 halfWay = normalize(lightDir+eyeDir);
    float spec = pow(max(dot(halfWay,transfNormal), 0.0),10.0);
    fragColor = ambientComponent + diff*diffuseComponent + spec*specComponent;
}