#version 300 es 
precision highp float;

out vec4 fragmentColor;
in vec4 tex;
//LABTODO: world space inputs
//LABDONE
in vec4 worldNormal;
in vec4 modelPosition;
in vec4 worldPosition;
in vec3 worldTangent;
in vec3 worldBitangent;

uniform struct{
  //LABTODO: uniform for surface color (diffuse refleciton coeff kd)
  //LABDONE
  sampler2D colorTexture;
  sampler2D normalTexture;
  //17. diahoz valo cuccok
  vec3 specularColor;
  float shininess;
} material;

uniform struct{
  mat4 viewProjMatrix;
  //LABTODO: uniform for computing view direction
  //LABDONE
  vec3 position;
} camera;

//LABTODO: uniforms for light source data
//LABDONE
//10. dia kod bemasolva
uniform struct {
  vec4 position;
  vec3 powerDensity;
  vec3 direction;
  vec2 cutoff;
} lights[8];

//11. dia shade kod bemasolva
//17. dia atirva
vec3 shade(vec3 normal, vec3 lightDir, vec3 viewDir, vec3 powerDensity, vec3 materialColor, vec3 specularColor, float shininess) {

  float cosa = max(dot(normal, lightDir), 0.0);
  float cosb = max(dot(viewDir, normal), 0.0);

  vec3 halfway = normalize(lightDir + viewDir);
  float cosDelta = max(dot(normal, halfway), 0.0);
  float specular = pow(cosDelta, shininess);

  return
    materialColor * cosa * powerDensity + specularColor * specular * (cosa / max(cosb, cosa)) * powerDensity;
}

void main(void) {
  //9. dia egyenlore konstans fenyirany
  //vec4 lightDir = vec4(0, 1, 0, 0);
  //9. dia fenyirany es normal kozotti koszinusz
  //float c = dot(lightDir, worldNormal);
  //if (c < 0.0) { c = 0; }
  fragmentColor = vec4(0.0);

  //17. diahoz cuccok
  vec3 viewDir = normalize(camera.position.xyz - worldPosition.xyz);

  vec3 baseColor = texture(material.colorTexture, tex.xy / tex.w).rgb;
  vec3 nmap = texture(material.normalTexture, tex.xy / tex.w).rgb;
  vec3 n_tangent = normalize(texture(material.normalTexture, tex.xy/tex.w).rgb * 2.0 - 1.0);
  mat3 TBN = mat3(
      normalize(worldTangent),
      normalize(worldBitangent),
      normalize(worldNormal.xyz)
  );
  vec3 n_world = normalize(TBN * n_tangent);
  //n_world = worldNormal.xyz;

  //11. dia for loop
  for (int i = 0; i < 8; ++i) {
      vec3 lightDir = lights[i].position.xyz - worldPosition.xyz * lights[i].position.w;
      vec3 L = normalize(lightDir);
      vec3 D =  normalize(lights[i].direction);
      if (dot(-L, D) >= lights[i].cutoff.x) {
          float spotFactor = 1.0;
          if (lights[i].cutoff.y > 0.0) {
              spotFactor = pow(max(dot(-L, D), 0.0), lights[i].cutoff.y);
          }
          vec3 powerDensity = lights[i].powerDensity;
          if (lights[i].position.w == 1.0) { //14. dia pontfenyforrasok
            powerDensity = lights[i].powerDensity / (4.0 * 3.141592);
          }
          powerDensity = powerDensity / (length(lightDir) * length(lightDir));
          powerDensity *= spotFactor;

          //vec3 shadeOut = shade(worldNormal.xyz,
            //lightDir, normalize(camera.position.xyz - worldPosition.xyz), powerDensity,
            //baseColor, material.specularColor, material.shininess);
          vec3 shadeOut = shade(n_world,
            lightDir, normalize(camera.position.xyz - worldPosition.xyz), powerDensity,
            baseColor, material.specularColor, material.shininess);
          if (shadeOut.x < 0.0) {
            shadeOut.x = 0.0;
          } if (shadeOut.y < 0.0) {
            shadeOut.y = 0.0;
          } if (shadeOut.z < 0.0) {
            shadeOut.z = 0.0;
          }
          fragmentColor.xyz += shadeOut;
      }
  }
  fragmentColor.w = 1.0;
  //9. dia fennyel szorzas
  //fragmentColor = texture(material.colorTexture, tex.xy/tex.w) * vec4(c, c, c, 1);
}
