#version 300 es 
precision highp float;

out vec4 fragmentColor;
in vec4 rayDir;

uniform struct {
	samplerCube envTexture;
	sampler2D noiseTexture;
} material;

//3. dia kod
/*float f(vec3 p){
  return p.y - texture(material.noiseTexture, p.xz * 0.01).r;
}*/

uniform struct {
  mat4 rayDirMatrix;
  vec3 position;
} camera;

float noise(vec3 r) {
  uvec3 s = uvec3(
    0x1D4E1D4E,
    0x58F958F9,
    0x129F129F);
  float f = 0.0;
  for(int i=0; i<16; i++) {
    vec3 sf = vec3(s & uvec3(0xFFFF)) / 65536.0 - vec3(0.5, 0.5, 0.5);

    f += sin(dot(sf, r));
    s = s >> 1;
  }
  return f / 32.0 + 0.5;
}

//18. dia kod
float f(vec3 p){
  return p.y - noise(p * 50.0);
}

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

//9. dia kod
/*vec3 fGrad(vec3 p){
  return vec3(
    f(p + vec3(+0.05, 0.0, 0.0) ) -
    f(p + vec3(-0.05, 0.0, 0.0) ) ,
    f(p + vec3(0.0, +0.05, 0.0) ) -
    f(p + vec3(0.0, -0.05, 0.0) ) ,
    f(p + vec3(0.0, 0.0, +0.05) ) -
    f(p + vec3(0.0, 0.0, -0.05) )
    );
}*/

//18. dia kod
vec3 fGrad(vec3 p){
  return vec3(0, 1, 0) - noiseGrad(p * 50.0);
}

void main(void) {
  vec3 d = normalize(rayDir.xyz);
  //4. dia kod
  float t1 = ((camera.position.y - 1.0f) / -d.y);//?sugar hol metszi az y=1 sikot?;
  float t2 = (camera.position.y / -d.y);//?sugar hol metszi az y=0 sikot?;
  float tstart = max(min(t1, t2), 0.0);
  float tend = max(t1, t2);
  bool found = false;
  vec3 p, color;

  //LABTODO: ray marching
  //LABDONE
  //6. dia kod
  if(tstart < tend) {
      p = camera.position + d * tstart;
      vec3 step = d * min((tend - tstart)/580.0, 0.01);
  // feladat:
  // ciklus fut 128-szor
  // p leptetese step-pel
  // ha f(p)<0 (atleptuk a 0 szintfeluletet):
  //        found=true; break;
    for (int i = 0; i < 128; ++i) {
      p += step;
      step *= 1.02f;
      if (f(p) < 0.0) {
        found = true;
        //step /= 2.0f;
        //p -= step;
        for (int e = 0; e < 16; ++e) {
          step /= 2.0f;
          if (f(p) < 0.0) {
            p -= step;
          } else {
            p += step;
          }
        }
        break;
      }
    }
  }

	if(!found){
	  color = texture(material.envTexture, d).rgb;
	} else {
	  color = normalize(fGrad(p));
	}

  //LABTODO: fog
  //LABDONE
  vec3 fogColor = vec3(1.0f, 0.9f, 0.25f);
  float csill = 200.0f; //ezek kicsik, igy jo, nagyon nem a feladat szerint
  float forr = 200.0f;
  vec3 fog_color = vec3(1.0f, 0.9f, 0.25f);

  float b = -7.0;
  float sigma_0 = csill;
  float q_0 = forr;

  float s = found ? length(camera.position - p) : 1e6;
  float y_ray_origin = camera.position.y;
  float y_ray_dir = d.y;

  float y_current = y_ray_origin + s * y_ray_dir;
  float sigma = sigma_0 * exp(b * y_current);
  float q = q_0 * exp(b * y_current);

  float optical_depth = (sigma_0 / (b * y_ray_dir)) * (exp(b * (y_ray_origin + s * y_ray_dir)) - exp(b * y_ray_origin));

  float transmittance = exp(-optical_depth);
  float source_contribution = (q_0 / sigma_0) * (1.0 - transmittance);

  color = color * transmittance + source_contribution * fog_color;

  fragmentColor = vec4(color, 1);
}
