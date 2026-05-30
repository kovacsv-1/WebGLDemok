#version 300 es 
//az 5. dia felatatait is ebben csinaltam
precision highp float;

out vec4 fragmentColor;
//LABTODO: world space inputs
//LABDONE
//3. dia vilagbeli normalvektor atvetele
in vec4 worldNormal;
in vec4 modelPosition;
in vec4 worldPosition;
in vec3 worldTangent;
in vec3 worldBitangent;
in vec4 tex;

uniform struct{
  //LABTODO: uniform for environment
  //LABDONE
  //5. dia FS, kell a kornyezet
  samplerCube envmapTexture;
  sampler2D normalTexture;
  float uTime;
} material;

uniform struct{
  mat4 viewProjMatrix;
  //LABTODO: uniform for computing view direction
  //LABDONE
  vec3 position;
} camera;

//7-8. dia iMSc feladat kodok bemasolva
float noise(vec3 r) {
  uvec3 s = uvec3(
    0x1D4E1D4E,
    0x58F958F9,
    0x129F129F);
  float f = 0.0;
  for(int i=0; i<16; i++) {
    vec3 sf =
    vec3(s & uvec3(0xFFFF))
  / 65536.0 - vec3(0.5, 0.5, 0.5);

    f += sin(dot(sf, r));
    s = s >> 1;
  }
  return f / 32.0 + 0.5;
}
vec3 noiseGrad(vec3 r) { //csak ezt hivom meg, a masik csak demo?
  uvec3 s = uvec3(
    0x1D4E1D4E,
    0x58F958F9,
    0x129F129F);
  vec3 f = vec3(0, 0, 0);
  for(int i=0; i<16; i++) {
    vec3 sf =
    vec3(s & uvec3(0xFFFF))
  / 65536.0 - vec3(0.5, 0.5, 0.5);

    f += cos(dot(sf, r)) * sf;
    s = s >> 1;
  }
  return f;
}


//LABTODO: uniforms for light source data

void main(void) {
  //5. dia arnyalt feluleti pont pozicioja
  vec3 x = worldPosition.xyz / worldPosition.w;
  vec3 viewDir = normalize(camera.position - x);
  viewDir *= vec3(1.0, 1.0, -1.0);
  vec3 nmap = texture(material.normalTexture, tex.xy / tex.w).rgb;
  vec3 n_tangent = normalize(texture(material.normalTexture, tex.xy/tex.w).rgb * 2.0 - 1.0);
  mat3 TBN = mat3(
    normalize(vec3(1.0, 0.0, 0.0)),
    normalize(vec3(0.0, 0.0, 1.0)),
    normalize(-worldNormal.xyz)
  );
  vec2 uv = tex.xy / tex.w;

  const float scale1 = 0.04;
  const float scale2 = 0.06;
  const float scale3 = 0.09;

  const vec2 scroll1 = vec2(0.1, 0.0);
  const vec2 scroll2 = vec2(0.0, 0.06);
  const vec2 scroll3 = vec2(0.04, 0.04);

  vec3 n1 = texture(material.normalTexture, uv * scale1 + scroll1 * material.uTime).rgb * 2.0 - 1.0;
  vec3 n2 = texture(material.normalTexture, uv * scale2 + scroll2 * material.uTime).rgb * 2.0 - 1.0;
  vec3 n3 = texture(material.normalTexture, uv * scale3 + scroll3 * material.uTime).rgb * 2.0 - 1.0;

  vec3 combinedNormal = normalize(n1 * 10.0 + n2 * 6.0 + n3 * 4.0);

  //3. dia normalizalas
  vec3 normal = -normalize(TBN * combinedNormal); // normalize(worldNormal.xyz); // /* + 7. dia iMSc intenzitas * noise */ + 0.25 * noiseGrad(worldPosition.xyz * 20.0f));
  //3. dia visszaadas
  //fragmentColor = vec4(abs(normal), 1);
  //5. dia olvassuk ki a kornyezetet tukoriranyban
  fragmentColor = texture( material.envmapTexture, reflect(viewDir, normal));
}
