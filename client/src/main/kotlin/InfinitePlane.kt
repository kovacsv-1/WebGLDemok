import org.khronos.webgl.WebGLRenderingContext as GL
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint16Array
import vision.gears.webglmath.Geometry
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class InfinitePlane(val gl : WebGL2RenderingContext) : Geometry() { //31. dia iMSc új geometria-típus
   
  val vertexBuffer = gl.createBuffer()
  init{
    gl.bindBuffer(GL.ARRAY_BUFFER, vertexBuffer)
    gl.bufferData(GL.ARRAY_BUFFER,
      Float32Array( arrayOf<Float>( //31. dia iMSc 4 koordináta/vertex
        0.0f, 0.0f, 0.0f, 1.0f, //31. dia iMSc 1 vertex az origóban
        (cos(0.0)).toFloat(), 0.0f, (sin(0.0)).toFloat(), 0.0f, //31. dia iMSc 3 vertex ideális pontokban körben
        (cos(2.0*PI/3)).toFloat(), 0.0f, (sin(2.0*PI/3)).toFloat(), 0.0f,
        (cos(4.0*PI/3)).toFloat(), 0.0f, (sin(4.0*PI/3)).toFloat(), 0.0f,
      )),
      GL.STATIC_DRAW)
  }

  val vertexNormalBuffer = gl.createBuffer()
  init{
    gl.bindBuffer(GL.ARRAY_BUFFER, vertexNormalBuffer)
    gl.bufferData(GL.ARRAY_BUFFER,
      Float32Array( arrayOf<Float>(
         0.0f,  0.0f, 1.0f,
         0.0f,  0.0f, 1.0f,
         0.0f,  0.0f, 1.0f,
         0.0f,  0.0f, 1.0f,
         0.0f,  0.0f, 1.0f
      )),
      GL.STATIC_DRAW)
  }

  val vertexTexCoordBuffer = gl.createBuffer()
  init{
    gl.bindBuffer(GL.ARRAY_BUFFER, vertexTexCoordBuffer)
    gl.bufferData(GL.ARRAY_BUFFER,
      Float32Array( arrayOf<Float>( //32. dia iMSc homogén textúra-koordináták, 4/vertex, y és z felcserélve
        0f, 0f, 0f, 1f,
        cos(0.0).toFloat(), sin(0.0).toFloat(), 0.0f, 0.0f,
        cos(2.0*PI/3).toFloat(), sin(2.0*PI/3).toFloat(), 0.0f, 0.0f,
        cos(4.0*PI/3).toFloat(), sin(4.0*PI/3).toFloat(), 0.0f, 0.0f,
      )),
      GL.STATIC_DRAW)
  }    

  val indexBuffer = gl.createBuffer()
  init{
    gl.bindBuffer(GL.ELEMENT_ARRAY_BUFFER, indexBuffer)
    gl.bufferData(GL.ELEMENT_ARRAY_BUFFER,
      Uint16Array( arrayOf<Short>(
        0, 1, 2, //legyezőszerű háromszögek (nem triangle_fan)
        0, 2, 3,
        0, 3, 1
      )),
      GL.STATIC_DRAW)
  }

  val inputLayout = gl.createVertexArray()
  init{
    gl.bindVertexArray(inputLayout)

    gl.bindBuffer(GL.ARRAY_BUFFER, vertexBuffer)
    gl.enableVertexAttribArray(0)
    gl.vertexAttribPointer(0,
      4, GL.FLOAT, //< four pieces of float
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
      4, GL.FLOAT, //< four pieces of float
      false, //< do not normalize (make unit length)
      0, //< tightly packed
      0 //< data starts at array start
    )    
    gl.bindVertexArray(null)
  }

  override fun draw() {
    gl.bindVertexArray(inputLayout)
    gl.bindBuffer(GL.ELEMENT_ARRAY_BUFFER, indexBuffer)

    gl.drawElements(GL.TRIANGLES, 9, GL.UNSIGNED_SHORT, 0)
  }
}