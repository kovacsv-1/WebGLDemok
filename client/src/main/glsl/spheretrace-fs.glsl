#version 300 es
precision highp float;

out vec4 fragmentColor;
in vec4 rayDir;

uniform struct {
  samplerCube envTexture;
  float objectCount;
  float minMode;
  float smoothFactor;
  float time;
  float twistMode;
  float lMode;
  float iters;
} material;

uniform struct {
  mat4 rayDirMatrix;
  vec3 position;
} camera;

uniform struct {
  vec3 shape;
  vec3 pos;
  vec3 size;
  mat4 rot;
  vec4 color;
} objects[64];

//rotation + translation
vec3 rotatePoint(vec3 p, mat4 r){
    return (r * vec4(p, 1.0)).xyz;
}
//pure rotation
vec3 rotateDir(vec3 v, mat4 r){
    return (r * vec4(v, 0.0)).xyz;
}

float smin_poly(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

float smin_exp(float a, float b, float k) {
    float res = exp(-k * a) + exp(-k * b);
    return -log(res) / k;
}

//min with hitindex tracking
float smoothUnion(float d1, float d2, int id1, int id2, out int hitId) {
  if (id1 < 49 && id2 < 49) {
    float k = material.smoothFactor * 0.2;
    float h = clamp(0.5 + 0.5 * (d2 - d1)/k, 0.0, 1.0);
    hitId = (h < 0.5) ? id1 : id2;
    return mix(d2, d1, h) - k*h*(1.0-h);
  } else if (material.minMode < 0.5) {
    if (d1 < d2) { hitId = id1; return d1; }
    else { hitId = id2; return d2; }
  } else {
    float k = material.smoothFactor;
    float h = clamp(0.5 + 0.5 * (d2 - d1)/k, 0.0, 1.0);
    hitId = (h < 0.5) ? id1 : id2;
    return mix(d2, d1, h) - k*h*(1.0-h);
  }
}

vec4 smoothColor(vec4 c1, vec4 c2, float d1, float d2, float k, int id1, int id2) {
  if (id1 < 49 && id2 < 49) {
    //float h = clamp(0.5 + 0.5 * (d2 - d1)/(k * 0.2), 0.0, 1.0);
    //return mix(c2, c1, h); // smooth union colour smoothing
    return (d1 < d2) ? c1 : c2;
  } else if (material.minMode < 0.5) {
    return (d1 < d2) ? c1 : c2;
  } else {
    float h = clamp(0.5 + 0.5 * (d2 - d1)/k, 0.0, 1.0);
    return mix(c2, c1, h); // smooth union colour smoothing
  }
}

//twist
vec3 twist(vec3 p) {
    float r = length(p.xy);
    if (r > 5.0) { //twistdomain
        return p;
    }

    float a = 1.0 * p.z + material.time * 0.5; //twistfactor

    float cosa = cos(a);
    float sina = sin(a);

    return vec3(
        p.x * cosa - p.y * sina,
        p.x * sina + p.y * cosa,
        p.z
    );
}

//SDF-ek
float SDF_sphere(vec3 p, vec3 pos, float radius) {
  return length(p - pos) - radius;
}

float SDF_plane(vec3 p, vec3 pos, vec3 normal) {
   return dot(p - pos, normal);
}

float SDF_box(vec3 p, vec3 pos, vec3 size, mat4 rot) {
  vec3 lp = rotatePoint(p - pos, rot);
  vec3 d = abs(lp) - size;
  return length(max(d, 0.0)) + min(max(d.x, max(d.y, d.z)), 0.0);
}

float SDF_cylinder(vec3 p, vec3 pos, float radius, float height, mat4 rot) {
  vec3 lp = rotatePoint(p - pos, rot);
  vec2 d = abs(vec2(length(lp.xy), lp.z)) - vec2(radius, height*0.5);
  return min(max(d.x, d.y), 0.0) + length(max(d, 0.0));
}

float SDF_torus(vec3 p, vec3 pos, vec2 radii, mat4 rot) {
  vec3 lp = rotatePoint(p - pos, rot);
  vec2 q = vec2(length(lp.xy) - radii.x, lp.z);
  return length(q) - radii.y;
}

float SDF(vec3 p, out int hitIndex, out vec4 col) {
  float minDist = 99999.9;
  int tempHit = -1;
  //vec3 col = objects[0].color;

  for(int i = (1 - int(material.lMode + 0.5f)) * 49; i < int(material.objectCount + 0.5f); i++) {
    float d = 99999.9;
    int s = int(objects[i].shape.x + 0.5);

    if(s == 0) d = SDF_sphere(p, objects[i].pos, objects[i].size.x);
    else if(s == 1) d = SDF_plane(p, objects[i].pos, rotateDir(vec3(0,1,0), objects[i].rot));
    else if(s == 2) d = SDF_box(p, objects[i].pos, objects[i].size, objects[i].rot);
    else if(s == 3) d = SDF_cylinder(p, objects[i].pos, objects[i].size.x, objects[i].size.y, objects[i].rot);
    else if(s == 4) d = SDF_torus(p, objects[i].pos, objects[i].size.xy, objects[i].rot);

    if (i == 0) {
      minDist = d;
      col = objects[0].color;
    } else {
      col = smoothColor(col, objects[i].color, minDist, d, material.smoothFactor, hitIndex, i);
      minDist = smoothUnion(minDist, d, hitIndex, i, tempHit);
      hitIndex = tempHit;
    }
  }

  return minDist;
}

//normals
vec3 get_normal(vec3 p, int hitIndex) {
  float eps = 0.001;
  vec3 n;
  vec4 col;
  n.x = SDF(p + vec3(eps,0,0), hitIndex, col) - SDF(p - vec3(eps,0,0), hitIndex, col);
  n.y = SDF(p + vec3(0,eps,0), hitIndex, col) - SDF(p - vec3(0,eps,0), hitIndex, col);
  n.z = SDF(p + vec3(0,0,eps), hitIndex, col) - SDF(p - vec3(0,0,eps), hitIndex, col);
  return normalize(n);
}

void main(void) {
  vec3 d = normalize(rayDir.xyz);
  vec3 color = texture(material.envTexture, d).rgb;
  float depth = 0.0;
  int hitIndex = 0;
  vec4 sumCol = vec4(0.0);
  for (int i=0; i < int(material.iters + 0.5f); ++i) { // felso limit nagyobb -> jobban nez ki, de jo kartyanak is nehez (ki gondolta volna)
    vec3 p = camera.position + d * depth;
    if (material.twistMode > 0.5f) p = twist(p);
    float dist = SDF(p, hitIndex, sumCol);
      if(dist < 0.001){
        vec3 n = get_normal(p, hitIndex);
        //color = texture(material.envTexture, reflect(d, n)).rgb * objects[hitIndex].color.w + objects[hitIndex].color.xyz * (1.0 - objects[hitIndex].color.w);
        color = texture(material.envTexture, reflect(d, n)).rgb * sumCol.w + sumCol.xyz * (1.0 - sumCol.w);
        break;
      }
      depth += dist * (1.0 / sqrt(4.0 + (3.14159 * 1.0) * (3.14159 * 1.0))); //1.0-k a twistfactor, ez a dist * (1 / lipschitz)
      if(depth > 100.0) break;
    }

  fragmentColor = vec4(color, 1);
}
