import org.w3c.dom.HTMLCanvasElement
import org.khronos.webgl.WebGLRenderingContext as GL
import org.khronos.webgl.Float32Array
import vision.gears.webglmath.UniformProvider
import vision.gears.webglmath.Vec3
import vision.gears.webglmath.Mat4
import kotlin.js.Date
import kotlin.math.abs
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

  val jsonLoader = JsonLoader()

  //chevy
  val chevyMeshes = arrayOf(
    Mesh(
      Material(texturedProgram).apply{
        this["colorTexture"]?.set(
          Texture2D(gl, "media/json/chevy/chevy.png"))
      }, jsonLoader.loadGeometries(gl,
        "media/json/chevy/chassis.json")[0]),
  )
  val wheelMesh = arrayOf(
    Mesh(
      Material(texturedProgram).apply{
        this["colorTexture"]?.set(
          Texture2D(gl, "media/json/chevy/chevy.png"))
      }, jsonLoader.loadGeometries(gl,
        "media/json/chevy/wheel.json")[0]),
  )

  val groundMesh = arrayOf (
    Mesh(
      Material(texturedProgram).apply{
        this["colorTexture"]?.set(
          Texture2D(gl, "media/json/road.png"))
      }, quadGeometry),
  )

  val treeMesh = arrayOf (
    Mesh(
      Material(texturedProgram).apply{
        this["colorTexture"]?.set(
          Texture2D(gl, "media/json/tree.png"))
      }, jsonLoader.loadGeometries(gl,
        "media/json/tree.json")[0]),
  )

  val gameObjects = ArrayList<GameObject>()
  val avatar = GameObject(*chevyMeshes).apply {
    move = object : GameObject.Motion(this) {
      override operator fun invoke(
        t: Float,
        dt: Float,
        keysPressed: Set<String>,
        gameObjects: List<GameObject>,
        spawn: List<GameObject>
      ): Boolean {
        //console.log(gameObject.position.x, gameObject.position.y, gameObject.position.z)
        var side = 0.0f
        var forward = 0.0f
        val facing = Vec3(sin(gameObject.yaw), 0.0f, cos(gameObject.yaw))
        val sideways = Vec3(sin(gameObject.yaw + 0.5f * 3.141592f), 0.0f, cos(gameObject.yaw + 0.5f * 3.141592f))
        val forwardness = facing.dot(gameObject.velocity)
        val sidewaysness = sideways.dot(gameObject.velocity)
        if ("A" in keysPressed) {side += 1}
        if ("D" in keysPressed) {side -= 1}
        if ("W" in keysPressed) {forward += 1}
        if ("S" in keysPressed) {forward -= 1}
        var brakes = 0.0f
        if (forward == 0.0f || forwardness * forward < 0.0f) {
          brakes = 0.5f
        }
        var onRoad = false
        if (gameObject.position.y >= 0.0f && gameObject.position.y + gameObject.velocity.y * dt <= 0.0f) {
          for (interactor in gameObjects) {
            if (abs(interactor.position.x - gameObject.position.x) < 60.0f && abs(interactor.position.z - gameObject.position.z) < 60.0f) {
              onRoad = true
              break
            }
          }
        }
        if (onRoad) {
          gameObject.velocity.set(gameObject.velocity.x, 0.0f, gameObject.velocity.z)
          gameObject.position.set(gameObject.position.x, 0.0f, gameObject.position.z)
          gameObject.velocity.set(gameObject.velocity + facing * dt * forward * 50.0f)
          if (gameObject.velocity.length() > 0.0f) {
            val velDir = Vec3(gameObject.velocity.x, gameObject.velocity.y, gameObject.velocity.z)
            velDir.normalize()
            val forwardSpeed = gameObject.velocity.length() * facing.dot(velDir)
            val sidewaysSpeed = gameObject.velocity.length() * sideways.dot(velDir)
            val forwardVel = facing * forwardSpeed - facing * forwardSpeed * 0.1f * dt
            val sidewaysVel = sideways * sidewaysSpeed - sideways * sidewaysSpeed * 0.9f * dt
            //gameObject.velocity.set((facing * forwardVel * 0.99f) + (sideways * 0.9f * sidewaysVel))
            gameObject.velocity.set((forwardVel + sidewaysVel) * (1.0f - brakes * dt))
          }

          if (velocity.length() > 0.0f) {
            gameObject.yaw += side /* gameObject.velocity.length() */ * dt * 0.01f * forwardness
          }
          if ("SPACE" in keysPressed) {
            gameObject.velocity.set(gameObject.velocity.x, 10f + 0.0f * gameObject.velocity.length() * 0.025f, gameObject.velocity.z)
          }
        }
        if (gameObject.position.y < -15.0f) {
          gameObject.position.set(0.0f, 0.0f, 0.0f)
          gameObject.velocity.set(0.0f, 0.0f, 0.0f)
          gameObject.pitch = 0.0f
          gameObject.yaw = 0.0f
          gameObject.roll = 0.0f
        }
        if (gameObject.velocity.length() > 1.0f) {
          gameObject.position += gameObject.velocity * dt
        }
        if (!onRoad) {
          gameObject.velocity += Vec3(0.0f, -10.0f, 0.0f) * dt
        }
        return true
      }
    }
  }
  val wheels = ArrayList<GameObject>()
  val road = ArrayList<GameObject>()
  val trees = ArrayList<GameObject>()
  init{
    gameObjects.add(avatar)
    wheels.add(GameObject(*wheelMesh).apply { position.set(7f, -3f, 13.75f); parent = avatar; move = object : GameObject.Motion(this) {
      val velocity = Vec3(0.0f, 0.0f, 0.0f) //19. dia gombokkal forgatható és mozgatható játékobjektum
      override operator fun invoke(
        t: Float,
        dt: Float,
        keysPressed: Set<String>,
        gameObjects: List<GameObject>,
        spawn: List<GameObject>
      ): Boolean {
        var side = 0.0f
        if ("A" in keysPressed) {side += 1}
        if ("D" in keysPressed) {side -= 1}
        gameObject.yaw = 1 * side
        gameObject.pitch += dt * 0.15f * Vec3(sin(gameObject.parent!!.yaw), 0.0f, cos(gameObject.parent!!.yaw)).dot(gameObject.parent!!.velocity)
        if (gameObject.pitch > 3.141592f) { gameObject.pitch -= 2.0f * 3.141592f }
        if (gameObject.pitch < -3.141592f) { gameObject.pitch += 2.0f * 3.141592f }
        return true
      }
    } })
    wheels.add(GameObject(*wheelMesh).apply { position.set(-7f, -3f, 13.75f); yaw = 3.141592f; parent = avatar; move = object : GameObject.Motion(this) {
      val velocity = Vec3(0.0f, 0.0f, 0.0f) //19. dia gombokkal forgatható és mozgatható játékobjektum
      override operator fun invoke(
        t: Float,
        dt: Float,
        keysPressed: Set<String>,
        gameObjects: List<GameObject>,
        spawn: List<GameObject>
      ): Boolean {
        var side = 0.0f
        if ("A" in keysPressed) {
          side += 1
        }
        if ("D" in keysPressed) {
          side -= 1
        }
        gameObject.yaw = 1 * side
        gameObject.pitch += dt * 0.15f * Vec3(sin(gameObject.parent!!.yaw), 0.0f, cos(gameObject.parent!!.yaw)).dot(
          gameObject.parent!!.velocity
        )
        if (gameObject.pitch > 3.141592f) {
          gameObject.pitch -= 2.0f * 3.141592f
        }
        if (gameObject.pitch < -3.141592f) {
          gameObject.pitch += 2.0f * 3.141592f
        }
        return true
      }
      } }) //pitch is when moving forward/backwards, yaw is turning
    wheels.add(GameObject(*wheelMesh).apply { position.set(7f, -3f, -11f); parent = avatar; move = object : GameObject.Motion(this) {
      val velocity = Vec3(0.0f, 0.0f, 0.0f) //19. dia gombokkal forgatható és mozgatható játékobjektum
      override operator fun invoke(
        t: Float,
        dt: Float,
        keysPressed: Set<String>,
        gameObjects: List<GameObject>,
        spawn: List<GameObject>
      ): Boolean {
        gameObject.pitch += dt * 0.15f * Vec3(sin(gameObject.parent!!.yaw), 0.0f, cos(gameObject.parent!!.yaw)).dot(
          gameObject.parent!!.velocity
        )
        if (gameObject.pitch > 3.141592f) {
          gameObject.pitch -= 2.0f * 3.141592f
        }
        if (gameObject.pitch < -3.141592f) {
          gameObject.pitch += 2.0f * 3.141592f
        }
        return true
      }
      } })
    wheels.add(GameObject(*wheelMesh).apply { position.set(-7f, -3f, -11f); yaw = 3.141592f; parent = avatar; move = object : GameObject.Motion(this) {
      val velocity = Vec3(0.0f, 0.0f, 0.0f) //19. dia gombokkal forgatható és mozgatható játékobjektum
      override operator fun invoke(
        t: Float,
        dt: Float,
        keysPressed: Set<String>,
        gameObjects: List<GameObject>,
        spawn: List<GameObject>
      ): Boolean {
        gameObject.pitch += dt * 0.15f * Vec3(sin(gameObject.parent!!.yaw), 0.0f, cos(gameObject.parent!!.yaw)).dot(
          gameObject.parent!!.velocity
        )
        if (gameObject.pitch > 3.141592f) {
          gameObject.pitch -= 2.0f * 3.141592f
        }
        if (gameObject.pitch < -3.141592f) {
          gameObject.pitch += 2.0f * 3.141592f
        }
        return true
      }
      } })
    trees.add(GameObject(*treeMesh).apply{position.set(100.0f, 0.0f, 0.0f); scale.set(2.0f, 2.0f, 2.0f); move = object : GameObject.Motion(this) {
      val velocity = Vec3(0.0f, 0.0f, 0.0f) //19. dia gombokkal forgatható és mozgatható játékobjektum
      override operator fun invoke(
        t: Float,
        dt: Float,
        keysPressed: Set<String>,
        gameObjects: List<GameObject>,
        spawn: List<GameObject>
      ): Boolean {
        if (abs(avatar.position.x - gameObject.position.x) * abs(avatar.position.x - gameObject.position.x) + abs(avatar.position.z - gameObject.position.z) * abs(avatar.position.z - gameObject.position.z) < 500.0f) {
          val away = (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize()
          val awaySpeed = avatar.velocity.length() * avatar.velocity.normalize().dot(away)
          if (awaySpeed < 0.0f) {
            avatar.velocity.set(avatar.velocity - away * 2.0f * awaySpeed + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
          } else {
            avatar.velocity.set(avatar.velocity + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
          }
          }
        return true
      }
    } })
    trees.add(GameObject(*treeMesh).apply{position.set(-100.0f, 0.0f, 0.0f); scale.set(2.0f, 2.0f, 2.0f); move = object : GameObject.Motion(this) {
      val velocity = Vec3(0.0f, 0.0f, 0.0f) //19. dia gombokkal forgatható és mozgatható játékobjektum
      override operator fun invoke(
        t: Float,
        dt: Float,
        keysPressed: Set<String>,
        gameObjects: List<GameObject>,
        spawn: List<GameObject>
      ): Boolean {
        if (avatar.position.y >= -3.0f && abs(avatar.position.x - gameObject.position.x) * abs(avatar.position.x - gameObject.position.x) + abs(avatar.position.z - gameObject.position.z) * abs(avatar.position.z - gameObject.position.z) < 500.0f) {
          val away = (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize()
          val awaySpeed = avatar.velocity.length() * avatar.velocity.normalize().dot(away)
          if (awaySpeed < 0.0f) {
            avatar.velocity.set(avatar.velocity - away * 2.0f * awaySpeed + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
          } else {
            avatar.velocity.set(avatar.velocity + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
          }
        }
        return true
      }
    } })
    for (i in 0..10) {
      trees.add(GameObject(*treeMesh).apply{position.set(100.0f - i * 20.0f, 0.0f, 550.0f); scale.set(2.0f, 2.0f, 2.0f); move = object : GameObject.Motion(this) {
        val velocity = Vec3(0.0f, 0.0f, 0.0f) //19. dia gombokkal forgatható és mozgatható játékobjektum
        override operator fun invoke(
          t: Float,
          dt: Float,
          keysPressed: Set<String>,
          gameObjects: List<GameObject>,
          spawn: List<GameObject>
        ): Boolean {
          if (avatar.position.y >= -3.0f && abs(avatar.position.x - gameObject.position.x) * abs(avatar.position.x - gameObject.position.x) + abs(avatar.position.z - gameObject.position.z) * abs(avatar.position.z - gameObject.position.z) < 500.0f) {
            val away = (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize()
            val awaySpeed = avatar.velocity.length() * avatar.velocity.normalize().dot(away)
            if (awaySpeed < 0.0f) {
              avatar.velocity.set(avatar.velocity - away * 2.0f * awaySpeed + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
            } else {
              avatar.velocity.set(avatar.velocity + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
            }
          }
          return true
        }
      } })
      trees.add(GameObject(*treeMesh).apply{position.set(100.0f, 0.0f, 550.0f - i * 20.0f); scale.set(2.0f, 2.0f, 2.0f); move = object : GameObject.Motion(this) {
        val velocity = Vec3(0.0f, 0.0f, 0.0f) //19. dia gombokkal forgatható és mozgatható játékobjektum
        override operator fun invoke(
          t: Float,
          dt: Float,
          keysPressed: Set<String>,
          gameObjects: List<GameObject>,
          spawn: List<GameObject>
        ): Boolean {
          if (avatar.position.y >= -3.0f && abs(avatar.position.x - gameObject.position.x) * abs(avatar.position.x - gameObject.position.x) + abs(avatar.position.z - gameObject.position.z) * abs(avatar.position.z - gameObject.position.z) < 500.0f) {
            val away = (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize()
            val awaySpeed = avatar.velocity.length() * avatar.velocity.normalize().dot(away)
            if (awaySpeed < 0.0f) {
              avatar.velocity.set(avatar.velocity - away * 2.0f * awaySpeed + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
            } else {
              avatar.velocity.set(avatar.velocity + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
            }
          }
          return true
        }
      } })
      trees.add(GameObject(*treeMesh).apply{position.set(100.0f - i * 20.0f, 0.0f, -1300.0f); scale.set(2.0f, 2.0f, 2.0f); move = object : GameObject.Motion(this) {
        val velocity = Vec3(0.0f, 0.0f, 0.0f) //19. dia gombokkal forgatható és mozgatható játékobjektum
        override operator fun invoke(
          t: Float,
          dt: Float,
          keysPressed: Set<String>,
          gameObjects: List<GameObject>,
          spawn: List<GameObject>
        ): Boolean {
          if (avatar.position.y >= -3.0f && abs(avatar.position.x - gameObject.position.x) * abs(avatar.position.x - gameObject.position.x) + abs(avatar.position.z - gameObject.position.z) * abs(avatar.position.z - gameObject.position.z) < 500.0f) {
            val away = (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize()
            val awaySpeed = avatar.velocity.length() * avatar.velocity.normalize().dot(away)
            if (awaySpeed < 0.0f) {
              avatar.velocity.set(avatar.velocity - away * 2.0f * awaySpeed + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
            } else {
              avatar.velocity.set(avatar.velocity + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
            }
          }
          return true
        }
      } })
      trees.add(GameObject(*treeMesh).apply{position.set(100.0f, 0.0f, -1300.0f + i * 20.0f); scale.set(2.0f, 2.0f, 2.0f); move = object : GameObject.Motion(this) {
        val velocity = Vec3(0.0f, 0.0f, 0.0f) //19. dia gombokkal forgatható és mozgatható játékobjektum
        override operator fun invoke(
          t: Float,
          dt: Float,
          keysPressed: Set<String>,
          gameObjects: List<GameObject>,
          spawn: List<GameObject>
        ): Boolean {
          if (avatar.position.y >= -3.0f && abs(avatar.position.x - gameObject.position.x) * abs(avatar.position.x - gameObject.position.x) + abs(avatar.position.z - gameObject.position.z) * abs(avatar.position.z - gameObject.position.z) < 500.0f) {
            val away = (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize()
            val awaySpeed = avatar.velocity.length() * avatar.velocity.normalize().dot(away)
            if (awaySpeed < 0.0f) {
              avatar.velocity.set(avatar.velocity - away * 2.0f * awaySpeed + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
            } else {
              avatar.velocity.set(avatar.velocity + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
            }
          }
          return true
        }
      } })
      trees.add(GameObject(*treeMesh).apply{position.set(-100.0f - i * 20.0f, 0.0f, -1100.0f); scale.set(2.0f, 2.0f, 2.0f); move = object : GameObject.Motion(this) {
        val velocity = Vec3(0.0f, 0.0f, 0.0f) //19. dia gombokkal forgatható és mozgatható játékobjektum
        override operator fun invoke(
          t: Float,
          dt: Float,
          keysPressed: Set<String>,
          gameObjects: List<GameObject>,
          spawn: List<GameObject>
        ): Boolean {
          if (avatar.position.y >= -3.0f && abs(avatar.position.x - gameObject.position.x) * abs(avatar.position.x - gameObject.position.x) + abs(avatar.position.z - gameObject.position.z) * abs(avatar.position.z - gameObject.position.z) < 500.0f) {
            val away = (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize()
            val awaySpeed = avatar.velocity.length() * avatar.velocity.normalize().dot(away)
            if (awaySpeed < 0.0f) {
              avatar.velocity.set(avatar.velocity - away * 2.0f * awaySpeed + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
            } else {
              avatar.velocity.set(avatar.velocity + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
            }
          }
          return true
        }
      } })
      trees.add(GameObject(*treeMesh).apply{position.set(-100.0f, 0.0f, -1100.0f + i * 20.0f); scale.set(2.0f, 2.0f, 2.0f); move = object : GameObject.Motion(this) {
        val velocity = Vec3(0.0f, 0.0f, 0.0f) //19. dia gombokkal forgatható és mozgatható játékobjektum
        override operator fun invoke(
          t: Float,
          dt: Float,
          keysPressed: Set<String>,
          gameObjects: List<GameObject>,
          spawn: List<GameObject>
        ): Boolean {
          if (avatar.position.y >= -3.0f && abs(avatar.position.x - gameObject.position.x) * abs(avatar.position.x - gameObject.position.x) + abs(avatar.position.z - gameObject.position.z) * abs(avatar.position.z - gameObject.position.z) < 500.0f) {
            val away = (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize()
            val awaySpeed = avatar.velocity.length() * avatar.velocity.normalize().dot(away)
            if (awaySpeed < 0.0f) {
              avatar.velocity.set(avatar.velocity - away * 2.0f * awaySpeed + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
            } else {
              avatar.velocity.set(avatar.velocity + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
            }
          }
          return true
        }
      } })
      trees.add(GameObject(*treeMesh).apply{position.set(-900.0f - i * 20.0f, 0.0f, 350.0f); scale.set(2.0f, 2.0f, 2.0f); move = object : GameObject.Motion(this) {
        val velocity = Vec3(0.0f, 0.0f, 0.0f) //19. dia gombokkal forgatható és mozgatható játékobjektum
        override operator fun invoke(
          t: Float,
          dt: Float,
          keysPressed: Set<String>,
          gameObjects: List<GameObject>,
          spawn: List<GameObject>
        ): Boolean {
          if (avatar.position.y >= -3.0f && abs(avatar.position.x - gameObject.position.x) * abs(avatar.position.x - gameObject.position.x) + abs(avatar.position.z - gameObject.position.z) * abs(avatar.position.z - gameObject.position.z) < 500.0f) {
            val away = (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize()
            val awaySpeed = avatar.velocity.length() * avatar.velocity.normalize().dot(away)
            if (awaySpeed < 0.0f) {
              avatar.velocity.set(avatar.velocity - away * 2.0f * awaySpeed + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
            } else {
              avatar.velocity.set(avatar.velocity + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
            }
          }
          return true
        }
      } })
      trees.add(GameObject(*treeMesh).apply{position.set(-1100.0f, 0.0f, 350.0f - i * 20.0f); scale.set(2.0f, 2.0f, 2.0f); move = object : GameObject.Motion(this) {
        val velocity = Vec3(0.0f, 0.0f, 0.0f) //19. dia gombokkal forgatható és mozgatható játékobjektum
        override operator fun invoke(
          t: Float,
          dt: Float,
          keysPressed: Set<String>,
          gameObjects: List<GameObject>,
          spawn: List<GameObject>
        ): Boolean {
          if (avatar.position.y >= -3.0f && abs(avatar.position.x - gameObject.position.x) * abs(avatar.position.x - gameObject.position.x) + abs(avatar.position.z - gameObject.position.z) * abs(avatar.position.z - gameObject.position.z) < 500.0f) {
            val away = (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize()
            val awaySpeed = avatar.velocity.length() * avatar.velocity.normalize().dot(away)
            if (awaySpeed < 0.0f) {
              avatar.velocity.set(avatar.velocity - away * 2.0f * awaySpeed + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
            } else {
              avatar.velocity.set(avatar.velocity + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
            }
          }
          return true
        }
      } })
    }
    trees.add(GameObject(*treeMesh).apply{position.set(-650.0f, 0.0f, -350.0f); scale.set(2.0f, 2.0f, 2.0f); move = object : GameObject.Motion(this) {
      val velocity = Vec3(0.0f, 0.0f, 0.0f) //19. dia gombokkal forgatható és mozgatható játékobjektum
      override operator fun invoke(
        t: Float,
        dt: Float,
        keysPressed: Set<String>,
        gameObjects: List<GameObject>,
        spawn: List<GameObject>
      ): Boolean {
        if (abs(avatar.position.x - gameObject.position.x) * abs(avatar.position.x - gameObject.position.x) + abs(avatar.position.z - gameObject.position.z) * abs(avatar.position.z - gameObject.position.z) < 500.0f) {
          val away = (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize()
          val awaySpeed = avatar.velocity.length() * avatar.velocity.normalize().dot(away)
          if (awaySpeed < 0.0f) {
            avatar.velocity.set(avatar.velocity - away * 2.0f * awaySpeed + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
          } else {
            avatar.velocity.set(avatar.velocity + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
          }
        }
        return true
      }
    } })
    trees.add(GameObject(*treeMesh).apply{position.set(-650.0f, 0.0f, -550.0f); scale.set(2.0f, 2.0f, 2.0f); move = object : GameObject.Motion(this) {
      val velocity = Vec3(0.0f, 0.0f, 0.0f) //19. dia gombokkal forgatható és mozgatható játékobjektum
      override operator fun invoke(
        t: Float,
        dt: Float,
        keysPressed: Set<String>,
        gameObjects: List<GameObject>,
        spawn: List<GameObject>
      ): Boolean {
        if (avatar.position.y >= -3.0f && abs(avatar.position.x - gameObject.position.x) * abs(avatar.position.x - gameObject.position.x) + abs(avatar.position.z - gameObject.position.z) * abs(avatar.position.z - gameObject.position.z) < 500.0f) {
          val away = (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize()
          val awaySpeed = avatar.velocity.length() * avatar.velocity.normalize().dot(away)
          if (awaySpeed < 0.0f) {
            avatar.velocity.set(avatar.velocity - away * 2.0f * awaySpeed + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
          } else {
            avatar.velocity.set(avatar.velocity + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
          }
        }
        return true
      }
    } })
    trees.add(GameObject(*treeMesh).apply{position.set(-2100.0f, 0.0f, -850.0f); scale.set(2.0f, 2.0f, 2.0f); move = object : GameObject.Motion(this) {
      val velocity = Vec3(0.0f, 0.0f, 0.0f) //19. dia gombokkal forgatható és mozgatható játékobjektum
      override operator fun invoke(
        t: Float,
        dt: Float,
        keysPressed: Set<String>,
        gameObjects: List<GameObject>,
        spawn: List<GameObject>
      ): Boolean {
        if (abs(avatar.position.x - gameObject.position.x) * abs(avatar.position.x - gameObject.position.x) + abs(avatar.position.z - gameObject.position.z) * abs(avatar.position.z - gameObject.position.z) < 500.0f) {
          val away = (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize()
          val awaySpeed = avatar.velocity.length() * avatar.velocity.normalize().dot(away)
          if (awaySpeed < 0.0f) {
            avatar.velocity.set(avatar.velocity - away * 2.0f * awaySpeed + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
          } else {
            avatar.velocity.set(avatar.velocity + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
          }
        }
        return true
      }
    } })
    trees.add(GameObject(*treeMesh).apply{position.set(-2100.0f, 0.0f, -1050.0f); scale.set(2.0f, 2.0f, 2.0f); move = object : GameObject.Motion(this) {
      val velocity = Vec3(0.0f, 0.0f, 0.0f) //19. dia gombokkal forgatható és mozgatható játékobjektum
      override operator fun invoke(
        t: Float,
        dt: Float,
        keysPressed: Set<String>,
        gameObjects: List<GameObject>,
        spawn: List<GameObject>
      ): Boolean {
        if (avatar.position.y >= -3.0f && abs(avatar.position.x - gameObject.position.x) * abs(avatar.position.x - gameObject.position.x) + abs(avatar.position.z - gameObject.position.z) * abs(avatar.position.z - gameObject.position.z) < 500.0f) {
          val away = (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize()
          val awaySpeed = avatar.velocity.length() * avatar.velocity.normalize().dot(away)
          if (awaySpeed < 0.0f) {
            avatar.velocity.set(avatar.velocity - away * 2.0f * awaySpeed + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
          } else {
            avatar.velocity.set(avatar.velocity + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
          }
        }
        return true
      }
    } })
    trees.add(GameObject(*treeMesh).apply{position.set(-1900.0f, 0.0f, -1200.0f); scale.set(2.0f, 2.0f, 2.0f); move = object : GameObject.Motion(this) {
      val velocity = Vec3(0.0f, 0.0f, 0.0f) //19. dia gombokkal forgatható és mozgatható játékobjektum
      override operator fun invoke(
        t: Float,
        dt: Float,
        keysPressed: Set<String>,
        gameObjects: List<GameObject>,
        spawn: List<GameObject>
      ): Boolean {
        if (abs(avatar.position.x - gameObject.position.x) * abs(avatar.position.x - gameObject.position.x) + abs(avatar.position.z - gameObject.position.z) * abs(avatar.position.z - gameObject.position.z) < 500.0f) {
          val away = (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize()
          val awaySpeed = avatar.velocity.length() * avatar.velocity.normalize().dot(away)
          if (awaySpeed < 0.0f) {
            avatar.velocity.set(avatar.velocity - away * 2.0f * awaySpeed + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
          } else {
            avatar.velocity.set(avatar.velocity + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
          }
        }
        return true
      }
    } })
    trees.add(GameObject(*treeMesh).apply{position.set(-1700.0f, 0.0f, -1200.0f); scale.set(2.0f, 2.0f, 2.0f); move = object : GameObject.Motion(this) {
      val velocity = Vec3(0.0f, 0.0f, 0.0f) //19. dia gombokkal forgatható és mozgatható játékobjektum
      override operator fun invoke(
        t: Float,
        dt: Float,
        keysPressed: Set<String>,
        gameObjects: List<GameObject>,
        spawn: List<GameObject>
      ): Boolean {
        if (avatar.position.y >= -3.0f && abs(avatar.position.x - gameObject.position.x) * abs(avatar.position.x - gameObject.position.x) + abs(avatar.position.z - gameObject.position.z) * abs(avatar.position.z - gameObject.position.z) < 500.0f) {
          val away = (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize()
          val awaySpeed = avatar.velocity.length() * avatar.velocity.normalize().dot(away)
          if (awaySpeed < 0.0f) {
            avatar.velocity.set(avatar.velocity - away * 2.0f * awaySpeed + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
          } else {
            avatar.velocity.set(avatar.velocity + (Vec3(avatar.position.x, 0.0f, avatar.position.z) - gameObject.position).normalize() * 15.0f)
          }
        }
        return true
      }
    } })
    for (tree in trees) {
      gameObjects.add(tree)
    }
    for (wh in wheels) {
      gameObjects.add(wh)
    }
    road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-50f, -6f, -1000f - 0.5f * 100f); scale.set(50f, 50f, 0f) })
    road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(50f, -6f,  -1000f - 0.5f * 100f); scale.set(50f, 50f, 0f) })
    for (i in 1..15) {
      road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-50f, -6f, -1000f + i * 100f); scale.set(50f, 50f, 0f) })
      road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(50f, -6f,  -1000f + i * 100f); scale.set(50f, 50f, 0f) })
    }
    for (i in 0..12) {
      //road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - i * 100f, -6f, -1000f + 16 * 100f); scale.set(50f, 50f, 0f) })
      road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - i * 100f, -6f, -1000f + 15 * 100f); scale.set(50f, 50f, 0f) })
      road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - i * 100f, -6f,  -1000f + 14 * 100f); scale.set(50f, 50f, 0f) })
    }
    for (i in 0..8) {
      road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - 12 * 100f, -6f, -1000f + (15 - i) * 100f); scale.set(50f, 50f, 0f) })
      road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - 11 * 100f, -6f, -1000f + (15 - i) * 100f); scale.set(50f, 50f, 0f) })
      road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - 10 * 100f, -6f,  -1000f + (15 - i) * 100f); scale.set(50f, 50f, 0f) })
    }
    for (i in 5..12) {
      //road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - i * 100f, -6f, -1000f + 16 * 100f); scale.set(50f, 50f, 0f) })
      road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - i * 100f, -6f, -1000f + 8 * 100f); scale.set(50f, 50f, 0f) })
      road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - i * 100f, -6f,  -1000f + 7 * 100f); scale.set(50f, 50f, 0f) })
    }
    for (i in 7..11) {
      road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - 6 * 100f, -6f, -1000f + (15 - i) * 100f); scale.set(50f, 50f, 0f) })
      road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - 5 * 100f, -6f, -1000f + (15 - i) * 100f); scale.set(50f, 50f, 0f) })
      road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - 4 * 100f, -6f,  -1000f + (15 - i) * 100f); scale.set(50f, 50f, 0f) })
    }
    for (i in 5..20) {
      road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - i * 100f, -6f, -1000f + 3 * 100f); scale.set(50f, 50f, 0f) })
    }
    for (i in 0..5) {
      road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - 20 * 100f, -6f, -1000f + (3 - i) * 100f); scale.set(50f, 50f, 0f) })
      road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - 19 * 100f, -6f, -1000f + (3 - i) * 100f); scale.set(50f, 50f, 0f) })
    }
    for (i in 0..6) {
      road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - (20 - i) * 100f, -6f, -1000f - 2 * 100f); scale.set(50f, 50f, 0f) })
    }
    road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - (20 - 7) * 100f, -6f, -1000f - 1.5f * 100f); scale.set(50f, 50f, 0f) })
    road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - (20 - 7) * 100f, -6f, -1000f - 2.5f * 100f); scale.set(50f, 50f, 0f) })
    road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - (20 - 8) * 100f, -6f, -1000f - 2 * 100f); scale.set(50f, 50f, 0f) })
    road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - (20 - 8) * 100f, -6f, -1000f - 3 * 100f); scale.set(50f, 50f, 0f) })
    road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - (20 - 8) * 100f, -6f, -1000f - 1 * 100f); scale.set(50f, 50f, 0f) })
    for (i in 0..3) {
      road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - (20 - 9 - i) * 100f, -6f, -1000f - 1 * 100f); scale.set(50f, 50f, 0f) })
      road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - (20 - 9 - i) * 100f, -6f, -1000f - 3 * 100f); scale.set(50f, 50f, 0f) })
    }
    road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - (20 - 13) * 100f, -6f, -1000f - 2 * 100f); scale.set(50f, 50f, 0f) })
    road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - (20 - 13) * 100f, -6f, -1000f - 3 * 100f); scale.set(50f, 50f, 0f) })
    road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - (20 - 13) * 100f, -6f, -1000f - 1 * 100f); scale.set(50f, 50f, 0f) })
    for (i in 14..22) {
      road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - (20 - i) * 100f, -6f, -1000f - 1.5f * 100f); scale.set(50f, 50f, 0f) })
      road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-150f - (20 - i) * 100f, -6f, -1000f - 2.5f * 100f); scale.set(50f, 50f, 0f) })
    }

    /*road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(50f, -6f, 0f); scale.set(50f, 50f, 0f) })
    road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-50f, -6f, 100f); scale.set(50f, 50f, 0f) })
    road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(50f, -6f, 100f); scale.set(50f, 50f, 0f) })
    road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(-50f, -6f, -100f); scale.set(50f, 50f, 0f) })
    road.add(GameObject(*groundMesh).apply { pitch = -0.5f * 3.141592f; position.set(50f, -6f, -100f); scale.set(50f, 50f, 0f) })
    */
    for (ob in road) {
      gameObjects.add(ob)
    }
  }

  val flipQuadGeometry = FlipQuadGeometry(gl)
  //LABTODO: resources for multipass rendering

  //LABTODO: lights
  //LABDONE
  //11. dia 2 fenyforras beallitase sceneben kod bemasolva es modositva
  //val lights = Array<Light>(3) { Light(it, *Program.all) }
  /*init{
    lights[0].position.set(0.0f, 0.0f, 1.0f, 0.0f).normalize();
    lights[0].powerDensity.set(1.0f, 1.0f, 0.0f);

    lights[1].position.set(0.0f, 1.0f, 0.0f, 0.0f).normalize();
    lights[1].powerDensity.set(1.0f, 1.0f, 1.0f); //11. dia eltero teljesitmenysuruseg-spektrum

    //15. dia egy pontfenyforras felvetele
    lights[2].position.set(-30.0f, 0.0f, 0.0f, 1.0f);
    lights[2].powerDensity.set(100.0f, 0.0f, 0.0f);
  }*/

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

    //LABTODO: create and bind framebuffer resources
  }

  fun update(gl : WebGL2RenderingContext, keysPressed : Set<String>) {

    val timeAtThisFrame = Date().getTime() 
    val dt = (timeAtThisFrame - timeAtLastFrame).toFloat() / 1000.0f
    val t  = (timeAtThisFrame - timeAtFirstFrame).toFloat() / 1000.0f    
    timeAtLastFrame = timeAtThisFrame

    camera.move(dt, keysPressed, avatar)
    //lights[2].position.set(-25.0f + 10.0f * cos((timeAtThisFrame - timeAtFirstFrame) * 0.005).toFloat(), 0.0f, 10.0f * sin((timeAtThisFrame - timeAtFirstFrame) * 0.005).toFloat(), 1.0f)

    //LABTODO: set render target

    // clear the screen
    gl.clearColor(0.3f, 0.0f, 0.3f, 1.0f)
    gl.clearDepth(1.0f)
    gl.clear(GL.COLOR_BUFFER_BIT or GL.DEPTH_BUFFER_BIT)

    val spawn = ArrayList<GameObject>()
    val killList = ArrayList<GameObject>()    
    gameObjects.forEach { 
      if(!it.move(t, dt, keysPressed, /*gameObjects*/ road, spawn)){
        killList.add(it)
      }
    }
    killList.forEach{ gameObjects.remove(it) }
    spawn.forEach{ gameObjects.add(it) }

    gameObjects.forEach { it.update() }

    backgroundMesh.draw(camera)
    gameObjects.forEach { it.draw( camera, /* LABTODO: pass lights*/ /*LABDONE*/ /*11. dia kod bemasolva*/ /**lights*/ ) }

    //LABTODO: post processing
  }
}
