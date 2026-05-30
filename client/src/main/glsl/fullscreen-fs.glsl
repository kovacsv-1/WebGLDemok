#version 300 es

precision highp float;

in vec4 texCoord;
in vec4 rayDir; //25. dia FS megkapja VStol a sugariranyt

// kell egy sampler uniform
uniform struct { samplerCube envTexture; } material; //26. dia


out vec4 fragmentColor;

void main(void) {
  fragmentColor = texture(material.envTexture, rayDir.xyz); //25. dia textura cimzes es szin visszaadas;26. dia kodreszlet
}