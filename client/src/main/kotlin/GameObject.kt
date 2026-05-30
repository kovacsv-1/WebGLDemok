import vision.gears.webglmath.*
import kotlin.math.exp
import kotlin.math.PI
import kotlin.math.floor

open class GameObject(
  vararg val meshes : Mesh
   ) : UniformProvider("gameObject") {

  val position = Vec3()
  var pitch = 0.0f
  var yaw = 0.0f
  var roll = 0.0f
  val scale = Vec3(1.0f, 1.0f, 1.0f)
  val velocity = Vec3(0.0f, 0.0f, 0.0f)

  val modelMatrix by Mat4()

  var parent : GameObject? = null

  init { 
    addComponentsAndGatherUniforms(*meshes)
  }

  fun update() {
    modelMatrix.set().
      scale(scale).
      rotate(pitch, Vec3(1f, 0f, 0f)).
      rotate(yaw, Vec3(0f, 1f, 0f)).
      rotate(roll, Vec3(0f, 0f, 1f)).
      translate(position)
    parent?.let{ parent -> 
      modelMatrix *= parent.modelMatrix
    }
  }

  open class Motion (val gameObject : GameObject){
    open operator fun invoke(
      dt : Float = 0.016666f, 
      t : Float = 0.0f, 
      keysPressed : Set<String> = emptySet<String>(), 
      gameObjects : List<GameObject> = emptyList<GameObject>(),
      spawn : List<GameObject> = emptyList<GameObject>()
      ) : Boolean { 
        return true 
    }
  }
  var move = Motion(this)

}
