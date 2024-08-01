#version 300 es

layout(location = 1) in vec3 vPos;
layout(location = 2) in vec3 offset;
layout(location = 3) in vec3 color;

uniform mat4 MVP;

out vec3 colorVarying;

void main(){
    vec3 pos = vPos + offset;
    gl_Position = MVP * vec4(pos,1);

    colorVarying = color;
}