#version 300 es

in vec4 vertexPosition;
in vec3 vertexNormal;
in vec4 vertexTexCoord;
uniform struct{
	mat4 modelMatrix;
	mat4 modelMatrixInverse;	
} gameObject;

uniform struct{
	mat4 viewProjMatrix;
  vec3 position;
} camera;

out vec4 texCoord;
out vec3 normal;
out vec4 worldPosition;
out vec4 worldNormal;

void main(void) {
  gl_Position = vertexPosition * gameObject.modelMatrix * camera.viewProjMatrix;
  texCoord = vertexTexCoord;
  worldPosition = vertexPosition * gameObject.modelMatrix;
  worldNormal = gameObject.modelMatrixInverse * vec4(vertexNormal, 0);
}