#version 300 es
precision highp float;

out vec4 fragmentColor;
in vec4 rayDir;

uniform struct {
    samplerCube envTexture;
    sampler2D noiseTexture;
} material;

uniform struct {
  mat4 rayDirMatrix;
  vec3 position; // camera position in world space
} camera;

uniform float time;         // seconds
uniform float sceneScale;   // global scale (>= 0.0001)
uniform float twistAmp;     // twist 'a' factor (Hart)
uniform int useSMin;        // 0 = plain min, 1 = smooth smin
uniform int maxSteps;
uniform float hitEpsilon;   // e.g. 1e-3
uniform int atomCount;      // <= 64
uniform vec3 atomPos[64];
uniform vec3 atomColor[64];

// ---------------- SDF primitives ----------------
float sdSphere(vec3 p, float r){ return length(p) - r; }
float sdPlane(vec3 p, vec3 n, float h){ return dot(p, n) + h; }
float sdTorus(vec3 p, vec2 t){
    vec2 q = vec2(length(p.xz) - t.x, p.y);
    return length(q) - t.y;
}

// smooth min (iq)
float smin_k(float a, float b, float k) {
    float h = clamp(0.5 + 0.5*(b - a)/k, 0.0, 1.0);
    return mix(b, a, h) - k*h*(1.0 - h);
}

// ---------------- twist deformation ----------------
// apply twist: rotate xy by angle = a * z
vec3 twist_forward(vec3 p, float a){
    float ang = a * p.z;
    float ca = cos(ang), sa = sin(ang);
    mat2 R = mat2(ca, -sa, sa, ca);
    vec2 xy = R * p.xy;
    return vec3(xy, p.z);
}
// inverse twist: rotate xy by -a*z
vec3 twist_inverse(vec3 p, float a){
    float ang = -a * p.z;
    float ca = cos(ang), sa = sin(ang);
    mat2 R = mat2(ca, -sa, sa, ca);
    vec2 xy = R * p.xy;
    return vec3(xy, p.z);
}

// ---------------- scene SDF (in twisted object-space) ----------------
float sceneSDF_unioned(vec3 p){ // p expected in object (untwisted) space
    // moving small sphere
    vec3 sPos = vec3(0.7 * sin(time * 1.25), 0.5 * cos(time * 0.9), 0.25 * sin(time * 0.6));
    float ds = sdSphere(p - sPos, 0.45);

    // center sphere
    float dc = sdSphere(p, 0.55);

    // torus (move a bit in z)
    vec3 tPos = vec3(-1.0, 0.0, 0.2 * sin(time * 0.6));
    float dt = sdTorus(p - tPos, vec2(0.55, 0.18));

    // plane at y = -1.0
    float dp = sdPlane(p, vec3(0.0, 1.0, 0.0), 1.0);

    if(useSMin == 0){
        float m = min(min(dc, ds), dt);
        m = min(m, dp);
        for(int i = 0; i < 64; ++i){
            if(i >= atomCount) break;
            m = min(m, sdSphere(p - atomPos[i], 0.12));
        }
        return m;
    } else {
        float k = 0.22 * max(0.01, sceneScale); // scale smoothness with sceneScale a bit
        float m = smin_k(dc, ds, k);
        m = smin_k(m, dt, k);
        m = smin_k(m, dp, k);
        for(int i = 0; i < 64; ++i){
            if(i >= atomCount) break;
            m = smin_k(m, sdSphere(p - atomPos[i], 0.12), k * 0.6);
        }
        return m;
    }
}

// ---------------- numerical normal ----------------
vec3 estimateNormal(vec3 p){
    // eps scaled to scene and hitEpsilon
    float eps = max(1e-5, hitEpsilon * 0.5 * max(0.001, sceneScale));
    return normalize(vec3(
        sceneSDF_unioned(p + vec3(eps,0,0)) - sceneSDF_unioned(p - vec3(eps,0,0)),
        sceneSDF_unioned(p + vec3(0,eps,0)) - sceneSDF_unioned(p - vec3(0,eps,0)),
        sceneSDF_unioned(p + vec3(0,0,eps)) - sceneSDF_unioned(p - vec3(0,0,eps))
    ));
}

// ---------------- sphere tracing (distance-guided) ----------------
struct Hit {
    bool hit;
    vec3 pos;      // hit position in untwisted (object) space
    float t;       // ray distance
    int atomIndex; // -1 if not atom
};

Hit traceSphere(vec3 ro, vec3 rd){
    Hit res;
    res.hit = false;
    res.pos = vec3(0.0);
    res.t = 0.0;
    res.atomIndex = -1;

    float t = 0.0;
    float a = twistAmp;
    float safeScale = max(1e-4, sceneScale);
    float scaleAdj = safeScale * (1.0 + clamp(abs(a), 0.0, 4.0) * 0.5);
    int steps = min(maxSteps, 512); // clamp to sane upper bound
    float maxT = 200.0 * max(1.0, safeScale);

    for(int i = 0; i < steps; ++i){
        vec3 p_ws = ro + rd * t;
        vec3 p_obj = twist_inverse(p_ws, a);

        float dist = sceneSDF_unioned(p_obj);

        // if dist is slightly negative due to smin, allow small negatives but keep step safe
        float safeDist = max(dist, -hitEpsilon * 0.5);

        // step length scaled by scene and twist
        float step = max(0.0001, safeDist / scaleAdj);

        if(dist < hitEpsilon){
            res.hit = true;
            res.pos = p_obj;
            res.t = t;
            int found = -1;
            float minD = 1e6;
            for(int j = 0; j < 64; ++j){
                if(j >= atomCount) break;
                float dd = abs(length(p_obj - atomPos[j]) - 0.12);
                if(dd < minD){
                    minD = dd;
                    found = j;
                }
            }
            if(minD < max( hitEpsilon * 8.0, 0.01 * safeScale )) res.atomIndex = found;
            else res.atomIndex = -1;
            return res;
        }

        t += clamp(step, 0.0001, 8.0);
        if(t > maxT) break;
    }
    return res;
}

// ---------------- shading ----------------
vec3 shade(vec3 p_world, vec3 normal, vec3 viewDir, int atomIdx){
    vec3 base = vec3(0.7, 0.75, 0.9);
    if(atomIdx >= 0) base = atomColor[atomIdx];
    vec3 lightDir = normalize(vec3(-0.5, 0.8, -0.6));
    float L = max(dot(normal, lightDir), 0.0);
    vec3 col = base * (0.12 + 0.9 * L);
    vec3 refl = reflect(-viewDir, normal);
    vec3 env = texture(material.envTexture, refl).rgb;
    float fres = pow(1.0 - max(0.0, dot(normal, viewDir)), 3.0);
    col = mix(col, env, 0.35 * fres);
    return col;
}

// ---------------- main ----------------
void main(void){
    vec3 rd = normalize(rayDir.xyz);
    vec3 ro = camera.position;

    Hit h = traceSphere(ro, rd);

    vec3 color;
    if(!h.hit){
        color = texture(material.envTexture, rd).rgb;
    } else {
        vec3 hit_world = twist_forward(h.pos, twistAmp);
        vec3 n_obj = estimateNormal(h.pos);
        float ang = twistAmp * hit_world.z;
        mat2 R = mat2(cos(ang), -sin(ang), sin(ang), cos(ang));
        vec3 n_world = vec3(R * n_obj.xy, n_obj.z);
        vec3 viewDir = normalize(ro - hit_world);
        color = shade(hit_world, n_world, viewDir, h.atomIndex);
    }

    float dist = h.hit ? h.t : 100.0;
    float fogAmount = 1.0 - exp(-0.02 * dist * max(1.0, sceneScale));
    vec3 fogColor = vec3(0.95, 0.9, 0.75);
    color = mix(color, fogColor, clamp(fogAmount, 0.0, 1.0));

    fragmentColor = vec4(color, 1.0);
}
