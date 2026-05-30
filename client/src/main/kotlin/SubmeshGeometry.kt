import org.khronos.webgl.WebGLRenderingContext as GL
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint16Array
import vision.gears.webglmath.Geometry
import vision.gears.webglmath.Vec3

class SubmeshGeometry(val gl : WebGL2RenderingContext, val jsonMesh : JsonMesh) : Geometry() {

  val vertexBuffer = gl.createBuffer()
  init{
    gl.bindBuffer(GL.ARRAY_BUFFER, vertexBuffer)
    gl.bufferData(GL.ARRAY_BUFFER,
      Float32Array( jsonMesh.vertices ),
      GL.STATIC_DRAW)
  }

  val vertexNormalBuffer = gl.createBuffer()
  init{
    gl.bindBuffer(GL.ARRAY_BUFFER, vertexNormalBuffer)
    gl.bufferData(GL.ARRAY_BUFFER,
      Float32Array( jsonMesh.normals ),
      GL.STATIC_DRAW)
  }

  val vertexTexCoordBuffer = gl.createBuffer()
  init{
    gl.bindBuffer(GL.ARRAY_BUFFER, vertexTexCoordBuffer)
    gl.bufferData(GL.ARRAY_BUFFER,
      Float32Array( jsonMesh.texturecoords[0] ),
      GL.STATIC_DRAW)
  }

  /*val indexBuffer = gl.createBuffer()
  val indexCount = jsonMesh.faces.flatten().size
  init{
    val indexIterator = jsonMesh.faces.flatten().iterator()
    val indexArray = Array<Short>(indexCount) {indexIterator.next()}

    gl.bindBuffer(GL.ELEMENT_ARRAY_BUFFER, indexBuffer)
    gl.bufferData(GL.ELEMENT_ARRAY_BUFFER,
      Uint16Array( indexArray ),
      GL.STATIC_DRAW)
  }

  val inputLayout = gl.createVertexArray()
  init{
    gl.bindVertexArray(inputLayout)

    gl.bindBuffer(GL.ARRAY_BUFFER, vertexBuffer)
    gl.enableVertexAttribArray(0)
    gl.vertexAttribPointer(0,
      3, GL.FLOAT, //< three pieces of float
      false, //< do not normalize (make unit length)
      0, //< tightly packed
      0 //< data starts at array start
    )
    gl.bindBuffer(GL.ARRAY_BUFFER, vertexNormalBuffer)
    gl.enableVertexAttribArray(1)
    gl.vertexAttribPointer(1,
      3, GL.FLOAT, //< three pieces of float
      false, //< do not normalize (make unit length)
      0, //< tightly packed
      0 //< data starts at array start
    )
    gl.bindBuffer(GL.ARRAY_BUFFER, vertexTexCoordBuffer)
    gl.enableVertexAttribArray(2)
    gl.vertexAttribPointer(2,
      2, GL.FLOAT, //< two pieces of float
      false, //< do not normalize (make unit length)
      0, //< tightly packed
      0 //< data starts at array start
    )
    gl.bindVertexArray(null)
  }*/

  // --- NEW: compute tangents and bitangents ---
  val tangents = Array<Float>(jsonMesh.vertices.size) { 0.0f }
  val bitangents = Array<Float>(jsonMesh.vertices.size) { 0.0f }

  val faces = jsonMesh.faces
  init {
    for (face in faces) {
      val i0 = face[0] * 3
      val i1 = face[1] * 3
      val i2 = face[2] * 3

      val uv0x = jsonMesh.texturecoords[0][face[0] * 2]
      val uv0y = jsonMesh.texturecoords[0][face[0] * 2 + 1]
      val uv1x = jsonMesh.texturecoords[0][face[1] * 2]
      val uv1y = jsonMesh.texturecoords[0][face[1] * 2 + 1]
      val uv2x = jsonMesh.texturecoords[0][face[2] * 2]
      val uv2y = jsonMesh.texturecoords[0][face[2] * 2 + 1]

      val v0 = Vec3(jsonMesh.vertices[i0], jsonMesh.vertices[i0 + 1], jsonMesh.vertices[i0 + 2])
      val v1 = Vec3(jsonMesh.vertices[i1], jsonMesh.vertices[i1 + 1], jsonMesh.vertices[i1 + 2])
      val v2 = Vec3(jsonMesh.vertices[i2], jsonMesh.vertices[i2 + 1], jsonMesh.vertices[i2 + 2])

      val edge1 = v1 - v0
      val edge2 = v2 - v0
      val deltaUV1 = Vec3(uv1x - uv0x, uv1y - uv0y, 0f)
      val deltaUV2 = Vec3(uv2x - uv0x, uv2y - uv0y, 0f)

      val f = 1.0f / (deltaUV1.x * deltaUV2.y - deltaUV2.x * deltaUV1.y)
      val tangent = (edge1 * deltaUV2.y - edge2 * deltaUV1.y) * f
      val bitangent = (-edge1 * deltaUV2.x + edge2 * deltaUV1.x) * f

      for (i in listOf(face[0], face[1], face[2])) {
        tangents[i * 3 + 0] += tangent.x
        tangents[i * 3 + 1] += tangent.y
        tangents[i * 3 + 2] += tangent.z
        bitangents[i * 3 + 0] += bitangent.x
        bitangents[i * 3 + 1] += bitangent.y
        bitangents[i * 3 + 2] += bitangent.z
      }
    }

  // normalize tangents and bitangents
    for (i in 0 until tangents.size step 3) {
      val t = Vec3(tangents[i], tangents[i + 1], tangents[i + 2]).normalize()
      val b = Vec3(bitangents[i], bitangents[i + 1], bitangents[i + 2]).normalize()
      tangents[i] = t.x; tangents[i + 1] = t.y; tangents[i + 2] = t.z
      bitangents[i] = b.x; bitangents[i + 1] = b.y; bitangents[i + 2] = b.z
    }
  }

  // --- buffers ---
  val vertexTangentBuffer = gl.createBuffer()
  init{
    gl.bindBuffer(GL.ARRAY_BUFFER, vertexTangentBuffer)
    gl.bufferData(GL.ARRAY_BUFFER,
      Float32Array(tangents),
      GL.STATIC_DRAW)
  }

  val vertexBitangentBuffer = gl.createBuffer()
  init{
    gl.bindBuffer(GL.ARRAY_BUFFER, vertexBitangentBuffer)
    gl.bufferData(GL.ARRAY_BUFFER,
      Float32Array(bitangents),
      GL.STATIC_DRAW)
  }

  val indexBuffer = gl.createBuffer()
  val indexCount = jsonMesh.faces.flatten().size
  init{
    val indexIterator = jsonMesh.faces.flatten().iterator()
    val indexArray = Array<Short>(indexCount) {indexIterator.next()}

    gl.bindBuffer(GL.ELEMENT_ARRAY_BUFFER, indexBuffer)
    gl.bufferData(GL.ELEMENT_ARRAY_BUFFER,
      Uint16Array( indexArray ),
      GL.STATIC_DRAW)
  }

  val inputLayout = gl.createVertexArray()
  init {
    gl.bindVertexArray(inputLayout)

    gl.bindBuffer(GL.ARRAY_BUFFER, vertexBuffer)
    gl.enableVertexAttribArray(0)
    gl.vertexAttribPointer(
      0,
      3, GL.FLOAT,
      false,
      0,
      0
    )

    gl.bindBuffer(GL.ARRAY_BUFFER, vertexNormalBuffer)
    gl.enableVertexAttribArray(1)
    gl.vertexAttribPointer(
      1,
      3, GL.FLOAT,
      false,
      0,
      0
    )

    gl.bindBuffer(GL.ARRAY_BUFFER, vertexTexCoordBuffer)
    gl.enableVertexAttribArray(2)
    gl.vertexAttribPointer(
      2,
      2, GL.FLOAT,
      false,
      0,
      0
    )

    // --- NEW: bind tangent and bitangent ---
    gl.bindBuffer(GL.ARRAY_BUFFER, vertexTangentBuffer)
    gl.enableVertexAttribArray(3)
    gl.vertexAttribPointer(
      3,
      3, GL.FLOAT,
      false,
      0,
      0
    )

    gl.bindBuffer(GL.ARRAY_BUFFER, vertexBitangentBuffer)
    gl.enableVertexAttribArray(4)
    gl.vertexAttribPointer(
      4,
      3, GL.FLOAT,
      false,
      0,
      0
    )

    gl.bindVertexArray(null)
  }

  override fun draw() {
    gl.bindVertexArray(inputLayout)
    gl.bindBuffer(GL.ELEMENT_ARRAY_BUFFER, indexBuffer)

    gl.drawElements(GL.TRIANGLES, indexCount, GL.UNSIGNED_SHORT, 0)
  }
}
