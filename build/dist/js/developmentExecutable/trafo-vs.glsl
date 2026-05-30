#version 300 es

in vec4 vertexPosition;
in vec3 vertexNormal;
in vec4 vertexTexCoord;

uniform struct{
	mat4 modelMatrix;
	//LABTODO: uniform for transforming normals
	//LABDONE
	//2. dia uj uniform es gameObject property
	mat4 modelMatrixInverse;
} gameObject;

uniform struct{
	mat4 viewProjMatrix;
	//LABTODO: uniform for computing view direction
	//LABDONE
	vec3 position;
} camera;

out vec4 tex;
//LABTODO: world space outputs
//LABDONE
//2. dia uj kimenetek
out vec4 modelPosition;
out vec4 worldPosition;
//2. dia uj kimenet
out vec4 worldNormal;

void main(void) {
  gl_Position = vertexPosition * gameObject.modelMatrix * camera.viewProjMatrix;
  //2. dia uj kimenetek
  modelPosition = vertexPosition;
  worldPosition = vertexPosition * gameObject.modelMatrix;
  worldNormal = gameObject.modelMatrixInverse * vec4(vertexNormal, 0);
  tex = vertexTexCoord;
}