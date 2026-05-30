import vision.gears.webglmath.Mat4
import vision.gears.webglmath.UniformProvider
import vision.gears.webglmath.Vec3
import vision.gears.webglmath.Vec4

class Object(id : Int, vararg programs : Program) : UniformProvider("objects[$id]") {

  val shape by Vec3(0.0f, 0.0f, 0.0f)
  val pos by Vec3(0.0f, 0.0f, 0.0f)
  val size by Vec3(1.0f, 1.0f, 1.0f)
  val rot by Mat4(1.0f, 0.0f, 0.0f, 1.0f,
                              0.0f, 1.0f, 0.0f, 1.0f,
                              0.0f, 0.0f, 1.0f, 1.0f,
                              0.0f, 0.0f, 1.0f, 1.0f,)
  val color by Vec4(1.0f, 0.0f, 0.0f, 0.0f) //w = 1 -> reflect background

  init{
    addComponentsAndGatherUniforms(*programs)
  }

}