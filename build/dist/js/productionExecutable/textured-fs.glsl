#version 300 es

precision highp float;

in vec4 texCoord;
in vec3 normal;
out vec4 fragmentColor;

uniform struct {
	sampler2D colorTexture;
} material;


void main(void) {
  fragmentColor = texture(material.colorTexture, texCoord.xy/texCoord.w);
  // * 0.2 + vec4(normalize(normal.xyz), 1);
}
