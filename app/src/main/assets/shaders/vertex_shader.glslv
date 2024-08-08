#version 300 es

layout(location = 1) in vec3 vPos;
layout(location = 2) in vec3 offset;
layout(location = 3) in float texIndex;
layout(location = 4) in vec3 normal;

uniform mat4 MVP;
uniform mat4 modelM;
uniform mat4 inverseModelM;

out float varyingTexIndex;
out vec3 transfNormal;
out vec3 fragModel;


void main(){
    vec3 pos = vPos + offset;
    varyingTexIndex = texIndex;
    gl_Position = MVP * vec4(pos,1);

    transfNormal = normalize(vec3(inverseModelM * vec4(normal,1)));
    fragModel = vec3(modelM * vec4(pos,1));
}