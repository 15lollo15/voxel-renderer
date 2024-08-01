#version 300 es

layout(location = 1) in vec3 vPos;
layout(location = 2) in vec3 offset;

uniform mat4 MVP;

void main(){
    vec3 pos = vPos + offset;
    gl_Position = MVP * vec4(pos,1);
}