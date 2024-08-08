#version 300 es

precision mediump float;

in float varyingTexIndex;

uniform sampler2D tex;

out vec4 fragColor;

void main() {
    vec2 texCoord = vec2(varyingTexIndex, 0);
    fragColor = texture(tex, texCoord);
}