import org.w3c.dom.HTMLCanvasElement
import org.khronos.webgl.WebGLRenderingContext as GL
import org.khronos.webgl.Float32Array
import vision.gears.webglmath.UniformProvider
import vision.gears.webglmath.Vec3
import vision.gears.webglmath.Mat4
import kotlin.js.Date
import kotlin.math.cos
import kotlin.math.sin

class Scene (
  val gl : WebGL2RenderingContext) : UniformProvider("scene") {

  val vsTrafo = Shader(gl, GL.VERTEX_SHADER, "trafo-vs.glsl")
  val fsTextured = Shader(gl, GL.FRAGMENT_SHADER, "textured-fs.glsl")
  val fsEnvmapped = Shader(gl, GL.FRAGMENT_SHADER, "envmapped-fs.glsl")
  val fsMaxBlinn = Shader(gl, GL.FRAGMENT_SHADER, "maxblinn-fs.glsl")   
  val texturedProgram = Program(gl, vsTrafo, fsTextured)
  val envmappedProgram = Program(gl, vsTrafo, fsEnvmapped)
  val maxBlinnProgram = Program(gl, vsTrafo, fsMaxBlinn)  
  val vsQuad = Shader(gl, GL.VERTEX_SHADER, "quad-vs.glsl")
  val fsBackground = Shader(gl, GL.FRAGMENT_SHADER, "background-fs.glsl")
  val backgroundProgram = Program(gl, vsQuad, fsBackground)
  val backgroundMaterial = Material(backgroundProgram)
  val skyCubeTexture = TextureCube(gl,
      "media/posx512.jpg", "media/negx512.jpg",
      "media/posy512.jpg", "media/negy512.jpg",
      "media/posz512.jpg", "media/negz512.jpg"
    )
  init {
    backgroundMaterial["envTexture"]?.set( skyCubeTexture )
  }
  val quadGeometry = TexturedQuadGeometry(gl)
  val backgroundMesh = Mesh(backgroundMaterial, quadGeometry)

  //18. dia phong-blinn shader, program
  val fsPhongBlinn = Shader(gl, GL.FRAGMENT_SHADER, "phongblinn-fs.glsl")
  val phongBlinnProgram = Program(gl, vsTrafo, fsPhongBlinn)

  val slowpokeTexture = Texture2D(gl, "media/slowpoke/YadonDh.png")
  val normalTexture = Texture2D(gl, "media/NormalMap.png")
  val slowpokeEyeTexture = Texture2D(gl, "media/slowpoke/YadonEyeDh.png")  
  val slowpokeMaterial = Material(texturedProgram)
  init{
    slowpokeMaterial["colorTexture"]?.set(slowpokeTexture)
  }
  val slowpokeEyeMaterial = Material(texturedProgram)  
  init{
    slowpokeEyeMaterial["colorTexture"]?.set(slowpokeEyeTexture)
  }
  val jsonLoader = JsonLoader()
  val slowpokeGeometries = jsonLoader.loadGeometries(gl,
    "media/slowpoke/slowpoke.json")
  val slowpokeMeshes = arrayOf(
    Mesh(
      Material(texturedProgram).apply{
        this["colorTexture"]?.set(
          Texture2D(gl, "media/slowpoke/YadonDh.png"))
      }, slowpokeGeometries[0]),
    Mesh(
      Material(texturedProgram).apply{
        this["colorTexture"]?.set(
          Texture2D(gl, "media/slowpoke/YadonEyeDh.png"))
      }, slowpokeGeometries[1]),
  )


  val envmappedMaterial = Material(envmappedProgram)
  init{
    //LABTODO: set environment to env mapped material
    //LABDONE
    //5. dia allitsuk be a texturat
    envmappedMaterial["envmapTexture"]?.set(skyCubeTexture)
    envmappedMaterial["normalTexture"]?.set(Texture2D(gl, "media/WaterNormal.png"))
  }
  val shadedSlowpokeMaterial = Material(maxBlinnProgram)
  init{
    //LABTODO: set surface texture to shading material
    //LABDONE
    shadedSlowpokeMaterial["colorTexture"]?.set(
      Texture2D(gl, "media/slowpoke/YadonDh.png"))
    shadedSlowpokeMaterial["normalTexture"]?.set(normalTexture)
    shadedSlowpokeMaterial["shininess"]?.set(150f)
    shadedSlowpokeMaterial["specularColor"]?.set(50f, 50f, 50f)
  }
  val shadedSlowpokeEyeMaterial = Material(maxBlinnProgram)  
  init{
    //LABTODO: set surface texture to shading material
    //LABDONE
    shadedSlowpokeEyeMaterial["colorTexture"]?.set(
      Texture2D(gl, "media/slowpoke/YadonEyeDh.png"))
    shadedSlowpokeEyeMaterial["normalTexture"]?.set(Texture2D(gl, "media/EyeNormal.png"))
    shadedSlowpokeEyeMaterial["shininess"]?.set(150f)
    shadedSlowpokeEyeMaterial["specularColor"]?.set(50f, 50f, 50f)
  }  
  val shadedSlowpokeMeshes = arrayOf( 
    Mesh(shadedSlowpokeMaterial, slowpokeGeometries[0]),
    Mesh(shadedSlowpokeEyeMaterial, slowpokeGeometries[1])
  )

  val gameObjects = ArrayList<GameObject>()
  init{
    val shadedObject0 = GameObject(*shadedSlowpokeMeshes)
    gameObjects += shadedObject0
      val shadedObject1 = GameObject(*shadedSlowpokeMeshes).apply{
          position.set(-10.0f)
      }
      gameObjects += shadedObject1
    val shadedObject2 = GameObject(*shadedSlowpokeMeshes).apply{
      position.set(10.0f)
    }
    gameObjects += shadedObject2
  }

  val flipQuadGeometry = FlipQuadGeometry(gl)
  //LABTODO: resources for multipass rendering

  //LABTODO: lights
  //LABDONE
  //11. dia 2 fenyforras beallitase sceneben kod bemasolva es modositva
  val lights = Array<Light>(5) { Light(it, *Program.all) }
  init{
    //15. dia egy pontfenyforras felvetele
    lights[0].position.set(0.0f, 30.0f, 0.0f, 1.0f);
    lights[0].cutoff.set(-1.0f, 20.0f) //cutoff.x az a nromalizált dot product amitnél nagyobbnak kell lennie, cutoff.y pedig a koszinuszt befolyásolja
    lights[0].powerDensity.set(200.0f, 200.0f, 200.0f);

    lights[1].position.set(0.0f, 1.0f, 0.0f, 0.0f).normalize();
    lights[1].powerDensity.set(1.0f, 1.0f, 1.0f);


    lights[2].position.set(0.0f, 30.0f, 0.0f, 1.0f);
    lights[2].cutoff.set(-1.0f, 20.0f)
    lights[2].powerDensity.set(0.0f, 125f, 0.0f);
    lights[2].direction.set(0.0f, -1.0f, 0.0f)


    lights[3].position.set(0.0f, 30.0f, 0.0f, 1.0f);
    lights[3].cutoff.set(-1.0f, 20.0f)
    lights[3].powerDensity.set(0.0f, 0.0f, 125f);
    lights[3].direction.set(Vec3(10.0f, -30.0f, 0.0f).normalize())


    lights[4].position.set(0.0f, 30.0f, 0.0f, 1.0f);
    lights[4].cutoff.set(-1.0f, 20.0f)
    lights[4].powerDensity.set(125f, 0.0f, 0.0f);
    lights[4].direction.set(Vec3(-10.0f, -30.0f, 0.0f).normalize())

    //lights[1].position.set(0.0f, 1.0f, 0.0f, 0.0f).normalize();
    //lights[1].powerDensity.set(1.0f, 1.0f, 0.0f); //11. dia eltero teljesitmenysuruseg-spektrum
  }

  val camera = PerspectiveCamera(*Program.all)

  val timeAtFirstFrame = Date().getTime()
  var timeAtLastFrame =  timeAtFirstFrame

  init{
    gl.enable(GL.DEPTH_TEST)
    addComponentsAndGatherUniforms(*Program.all)
  }

  //textured infinite plane
  val vsTextured = Shader(gl, GL.VERTEX_SHADER, "textured-vs.glsl")
  val fsGround = Shader(gl, GL.FRAGMENT_SHADER, "infinite-fs.glsl")
  val groundProgram = Program(gl, vsTextured, fsGround)

  val groundGeometry = InfinitePlane(gl)
  val groundMaterial = Material(groundProgram).apply{
    this["colorTexture"]?.set(
      Texture2D(gl, "media/floor.png"))
  }
  val groundObject = GameObject(Mesh(envmappedMaterial, groundGeometry))

  init {
    //gameObjects += groundObject
  }

  fun resize(gl : WebGL2RenderingContext, canvas : HTMLCanvasElement) {
    gl.viewport(0, 0, canvas.width, canvas.height)
    camera.setAspectRatio(canvas.width.toFloat() / canvas.height.toFloat())

    //LABTODO: create and bind framebuffer resources
  }

  fun update(gl : WebGL2RenderingContext, keysPressed : Set<String>) {

    val timeAtThisFrame = Date().getTime() 
    val dt = (timeAtThisFrame - timeAtLastFrame).toFloat() / 1000.0f
    val t  = (timeAtThisFrame - timeAtFirstFrame).toFloat() / 1000.0f
    timeAtLastFrame = timeAtThisFrame

    camera.move(dt, keysPressed)
    //lights[0].position.set(10.0f * cos((timeAtThisFrame - timeAtFirstFrame) * 0.005).toFloat(), 25.0f, 10.0f * sin((timeAtThisFrame - timeAtFirstFrame) * 0.005).toFloat(), 1.0f)
    lights[0].direction.set(Vec3(10.0f * sin(t), 0.0f, 0.0f) - lights[0].position.xyz)
    lights[0].direction.normalize()

    //LABTODO: set render target

    // clear the screen
    gl.clearColor(0.3f, 0.0f, 0.3f, 1.0f)
    gl.clearDepth(1.0f)
    gl.clear(GL.COLOR_BUFFER_BIT or GL.DEPTH_BUFFER_BIT)

    val spawn = ArrayList<GameObject>()
    val killList = ArrayList<GameObject>()    
    gameObjects.forEach { 
      if(!it.move(t, dt, keysPressed, gameObjects, spawn)){
        killList.add(it)
      }
    }
    killList.forEach{ gameObjects.remove(it) }
    spawn.forEach{ gameObjects.add(it) }

    gameObjects.forEach { it.update() }

    backgroundMesh.draw(camera)
    gameObjects.forEach { it.draw( camera, /* LABTODO: pass lights*/ /*LABDONE*/ /*11. dia kod bemasolva*/ *lights ) }

    envmappedMaterial["uTime"]?.set(t)
    groundObject.draw(camera, *lights)

    //LABTODO: post processing
  }
}
