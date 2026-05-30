#version 300 es

precision highp float;

in vec4 tex;
in vec3 normal;
out vec4 fragmentColor;

uniform struct {
	sampler2D colorTexture;
} material;


void main(void) {
  fragmentColor = texture(material.colorTexture, tex.xy/tex.w);
  // * 0.2 + vec4(normalize(normal.xyz), 1);
}
