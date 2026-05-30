#version 300 es 
precision highp float;

out vec4 fragmentColor;
in vec4 worldNormal;
in vec4 worldPosition;

uniform struct{
	samplerCube envmapTexture;
} material;

uniform struct{
  mat4 viewProjMatrix;
  vec3 position;
} camera;

uniform struct {
  vec4 position;
  vec3 powerDensity;
} lights[8];

vec3 noiseGrad(vec3 r) {
  uvec3 s =
    uvec3(0x1D4E1D4E, 0x58F958F9, 0x129F129F);
  vec3 f = vec3(0, 0, 0);
  for(int i=0; i<16; i++) {
    vec3 sf =
      vec3(s & uvec3(0xffff)) / 65536.0 
    - vec3(0.5, 0.5, 0.5);
    
    f += cos(dot(sf, r)) * sf;
    s = s >> 1;
  }
  return f;
}

void main(void) {
  vec3 normal = normalize(worldNormal.xyz);
  vec3 x = worldPosition.xyz / worldPosition.w;
  vec3 viewDir = normalize(camera.position - x);
  normal = normalize(normal + noiseGrad(x * 40.0) * 0.0001);

  fragmentColor = texture(material.envmapTexture, reflect(-viewDir, normal));
}
