import org.w3c.dom.HTMLCanvasElement
import org.khronos.webgl.WebGLRenderingContext as GL
import vision.gears.webglmath.UniformProvider
import vision.gears.webglmath.Vec3
import vision.gears.webglmath.Mat4
import vision.gears.webglmath.Vec4
import kotlin.js.Date
import kotlin.math.cos
import kotlin.math.sin

class Scene (
  val gl : WebGL2RenderingContext) : UniformProvider("scene") {

  val vsQuad = Shader(gl, GL.VERTEX_SHADER, "quad-vs.glsl")
  val fsSpheretrace = Shader(gl, GL.FRAGMENT_SHADER, "spheretrace-fs.glsl")

  val quadGeometry = TexturedQuadGeometry(gl)

  val spheretraceProgram = Program(gl, vsQuad, fsSpheretrace)
  val spheretraceMaterial = Material(spheretraceProgram)
  val skyCubeTexture = TextureCube(gl,
      "media/posx512.jpg", "media/negx512.jpg",
      "media/posy512.jpg", "media/negy512.jpg",
      "media/posz512.jpg", "media/negz512.jpg"
    )
  init {
    spheretraceMaterial["envTexture"]?.set( skyCubeTexture )
    spheretraceMaterial["minMode"]?.set(0.0f)
    spheretraceMaterial["smoothFactor"]?.set(0.5f)
  }
  val spheretraceMesh = Mesh(spheretraceMaterial, quadGeometry)


  //atom
  val atomColors = mapOf(
    1 to Vec4(1f, 1f, 1f, 0.0f),   // H fehér
    6 to Vec4(0.2f, 0.2f, 0.2f, 0.0f), // C fekete/szürke
    7 to Vec4(0f, 0f, 1f, 0.0f),   // N kék
    8 to Vec4(1f, 0f, 0f, 0.0f)    // O piros
  )

  val x = floatArrayOf(2.4802f,0.4729f,-5.1983f,4.1981f,-0.7385f,-0.6067f,-2.05f,1.8612f,1.6814f,-3.2208f,-1.8358f,-3.0277f,0.591f,-4.2667f,0.5883f,2.8655f,-4.5736f,-1.9503f,-4.3946f,-3.2076f,5.2755f,4.6102f,5.8541f,4.5924f,-0.8336f,-2.205f,-1.9844f,2.1425f,2.5606f,1.6242f,0.6777f,-0.1513f,1.5626f,0.4664f,-5.1544f,-1.0678f,-6.1978f,-5.3619f,-3.2646f,6.052f,4.8717f,5.6243f,3.9874f,5.0837f,6.2792f,6.6467f,3.6067f,5.3016f,4.8777f)
  val y = floatArrayOf(1.6091f,-2.2418f,-0.2437f,0.4702f,-1.3851f,0.0201f,-2.1699f,-0.2866f,-1.4473f,-1.2547f,0.8415f,0.1264f,0.4819f,0.7466f,-2.9739f,0.6842f,-1.4628f,2.2203f,2.1182f,2.8404f,1.3378f,-0.6392f,0.8463f,-0.2378f,-1.1669f,-2.989f,-2.6039f,-0.6596f,-2.1019f,-1.0642f,1.4409f,-3.7795f,-3.4714f,-2.3344f,-2.3592f,2.8397f,-0.0995f,2.6073f,3.9129f,1.4046f,2.3457f,-0.9446f,-1.5218f,0.8028f,-0.1572f,1.519f,0.1092f,0.5742f,-1.0876f)
  val z = floatArrayOf(1.0171f,0.4751f,0.1998f,-0.0574f,0.5912f,-0.0673f,0.2259f,-0.2462f,0.7414f,0.2186f,-0.1843f,-0.0011f,-0.4721f,-0.0259f,-0.7864f,0.297f,0.3455f,-0.4077f,-0.2476f,-0.4409f,0.4008f,-0.9137f,1.7169f,-2.3788f,1.6679f,0.9378f,-0.7784f,-1.2324f,0.7495f,1.7708f,-0.9767f,-0.836f,-0.8601f,-1.6667f,0.5115f,-0.5437f,0.2397f,-0.2693f,-0.6133f,-0.3685f,0.5488f,-0.6299f,-0.7642f,2.494f,1.6136f,2.0585f,-2.7018f,-2.571f,-3.0068f)

  val elements = intArrayOf(8,7,7,7,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1)

  var minMode = 0
  var twistMode = 0
  var lMode = 0
  var iters = 128
  var objects = Array<Object>(54) { Object(it, *Program.all) }
  init{
    for (i in 0..<49) {
      objects[i].shape.set(Vec3(0f)) // gömb
      objects[i].pos.set(Vec3(x[i], y[i] + 10.0f, z[i]) * 0.3f)
      objects[i].size.set(Vec3(if(elements[i]==1) 0.2f else 0.3f)) // kisebb a H
      objects[i].color.set(atomColors[elements[i]] ?: Vec4(1f,1f,1f,0.0f))
    }

    objects[49].shape.set(Vec3(0.0f))
    objects[49].pos.set(0.0f, 0.0f, 0.0f)
    objects[49].size.set(1.0f, 1.0f, 1.0f)
    objects[49].rot.set(Mat4(1.0f, 0.0f, 0.0f, 1.0f,
      0.0f, 1.0f, 0.0f, 1.0f,
      0.0f, 0.0f, 1.0f, 1.0f,
      0.0f, 0.0f, 1.0f, 1.0f,))
    objects[49].color.set(0.0f, 0.0f, 0.0f, 1.0f)

    objects[50].shape.set(Vec3(4.0f))
    objects[50].pos.set(5.0f, 1.0f, 0.0f)
    objects[50].size.set(1.0f, 0.5f, 1.0f)
    objects[50].rot.set(Mat4(1.0f, 0.0f, 0.0f, 1.0f,
      0.0f, 1.0f, 0.0f, 1.0f,
      0.0f, 0.0f, 1.0f, 1.0f,
      0.0f, 0.0f, 1.0f, 1.0f,))
    objects[50].color.set(0.75f, 0.55f, 0.25f, 0.25f)

    objects[51].shape.set(Vec3(1.0f))
    objects[51].pos.set(0.0f, -5.0f, 0.0f)
    objects[51].size.set(1.0f, 1.0f, 1.0f)
    objects[51].rot.set(Mat4(1.0f, 0.0f, 0.0f, 1.0f,
      0.0f, 1.0f, 0.0f, 1.0f,
      0.0f, 0.0f, 1.0f, 1.0f,
      0.0f, 0.0f, 1.0f, 1.0f,))
    objects[51].color.set(0.2f, 0.5f, 0.2f, 0.0f)

    objects[52].shape.set(Vec3(3.0f))
    objects[52].pos.set(0.0f, 0.0f, 0.0f)
    objects[52].size.set(1.0f, 1.0f, 1.0f)
    objects[52].rot.set(Mat4(1.0f, 0.0f, 0.0f, 1.0f,
      0.0f, 1.0f, 0.0f, 1.0f,
      0.0f, 0.0f, 1.0f, 1.0f,
      0.0f, 0.0f, 1.0f, 1.0f,))
    objects[52].color.set(0.75f, 0.5f, 0.5f, 0.75f)

    objects[53].shape.set(Vec3(2.0f))
    objects[53].pos.set(5.0f, 1.0f, 0.0f)
    objects[53].size.set(1.0f, 0.5f, 1.0f)
    objects[53].rot.set(Mat4(1.0f, 0.0f, 0.0f, 1.0f,
      0.0f, 1.0f, 0.0f, 1.0f,
      0.0f, 0.0f, 1.0f, 1.0f,
      0.0f, 0.0f, 1.0f, 1.0f,))
    objects[53].color.set(0.0f, 0.0f, 0.5f, 0.5f)
  }

  val camera = PerspectiveCamera(*Program.all)

  val timeAtFirstFrame = Date().getTime()
  var timeAtLastFrame =  timeAtFirstFrame

  init{
    gl.enable(GL.DEPTH_TEST)
    addComponentsAndGatherUniforms(*Program.all)
  }

  fun resize(gl : WebGL2RenderingContext, canvas : HTMLCanvasElement) {
    gl.viewport(0, 0, canvas.width, canvas.height)
    camera.setAspectRatio(canvas.width.toFloat() / canvas.height.toFloat())
  }

  var wasSpacePressed = false
  var wasShiftPressed = false
  var wasLPressed = false
  var wasNPressed = false
  var wasMPressed = false

  @Suppress("UNUSED_PARAMETER")
  fun update(gl : WebGL2RenderingContext, keysPressed : Set<String>) {

    val timeAtThisFrame = Date().getTime()
    val dt = (timeAtThisFrame - timeAtLastFrame).toFloat() / 1000.0f
    val t = (timeAtThisFrame - timeAtFirstFrame).toFloat() / 1000.0f
    timeAtLastFrame = timeAtThisFrame

    camera.move(dt, keysPressed)

    if ("SPACE" in keysPressed && !wasSpacePressed) {
      minMode += 1
      minMode %= 2
      wasSpacePressed = true
    }
    if (!("SPACE" in keysPressed)) wasSpacePressed = false
    if ("SHIFT" in keysPressed && !wasShiftPressed) {
      twistMode += 1
      twistMode %= 2
      wasShiftPressed = true
    }
    if (!("SHIFT" in keysPressed)) wasShiftPressed = false
    if ("L" in keysPressed && !wasLPressed) {
      lMode += 1
      lMode %= 2
      wasLPressed = true
    }
    if (!("L" in keysPressed)) wasLPressed = false
    if ("N" in keysPressed && !wasNPressed) {
      iters /= 2
      wasNPressed = true
    }
    if (!("N" in keysPressed)) wasNPressed = false
    if ("M" in keysPressed && !wasMPressed) {
      iters *= 2
      wasMPressed = true
    }
    if (!("M" in keysPressed)) wasMPressed = false
    iters = clamp(iters, 32, 1024)

    gl.clearColor(0.3f, 0.0f, 0.3f, 1.0f)
    gl.clearDepth(1.0f)
    gl.clear(GL.COLOR_BUFFER_BIT or GL.DEPTH_BUFFER_BIT)

    objects[objects.size - 5].pos.set(2.0f * sin(t), 0.0f, 2.0f * cos(t))
    objects[objects.size - 4].pos.set(-2.0f * sin(t), 1.0f, 2.0f * -cos(t))
    //objects[objects.size - 3].pos.set(0.0f, sin(t) - 5.0f, 0.0f)
    objects[objects.size - 2].pos.set(2.0f * sin(t), 1.0f, 2.0f * -cos(t))
    objects[objects.size - 1].pos.set(-2.0f * sin(t), 1.0f, 2.0f * cos(t))

    // draw
    spheretraceMaterial["objectCount"]?.set(objects.size.toFloat())
    spheretraceMaterial["minMode"]?.set(minMode.toFloat())
    spheretraceMaterial["time"]?.set(t)
    spheretraceMaterial["twistMode"]?.set(twistMode.toFloat())
    spheretraceMaterial["lMode"]?.set(lMode.toFloat())
    spheretraceMaterial["iters"]?.set(iters.toFloat())
    spheretraceMesh.draw(camera, this, *objects)

  }
}

private fun clamp(iters: Int, i2: Int, i3: Int): Int {
  return if (iters > i3) i3 else if (iters < i2) i2 else iters
}
