#version 300 es
//25. dia uj vs
in vec4 vertexPosition; //#vec4# A four-element vector [x,y,z,w].; We leave z and w alone.; They will be useful later for 3D graphics and transformations. #vertexPosition# attribute fetched from vertex buffer according to input layout spec
in vec4 vertexTexCoord;

uniform struct{
  mat4 modelMatrix;
} gameObject;

uniform struct{
  mat4 viewProjMatrix;
  mat4 rayDirMatrix;
} camera;

uniform struct {
  float time;
} scene;

out vec4 modelPosition;
out vec4 worldPosition;
out vec4 texCoord;
out vec4 rayDir;

void main(void) {
  modelPosition = vertexPosition;
  gl_Position = vec4(vertexPosition.xy, 0.99999f, vertexPosition.w); //25. dia z = 0.99999 minden mogott // * gameObject.modelMatrix * camera.viewProjMatrix; //25. dia nem transzformal
  worldPosition = gl_Position; //25. dia nem transzformal
  rayDir = worldPosition * camera.rayDirMatrix; //25. dia kiszamolja a sugariranyt
  texCoord = vertexTexCoord;
}