#version 300 es
//32. dia iMSc FS-beli uv szamitasra kulon FS
precision highp float;

in vec4 texCoord;

uniform struct {
  sampler2D colorTexture; 
} material;

out vec4 fragmentColor;

void main(void) {
  fragmentColor = texture(material.colorTexture, texCoord.xy / (texCoord.w / 0.1f));
}