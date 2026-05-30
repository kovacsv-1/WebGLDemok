import vision.gears.webglmath.UniformProvider
import vision.gears.webglmath.Geometry
import org.w3c.xhr.XMLHttpRequest
import org.w3c.dom.events.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class JsonMesh(
  val vertices : Array<Float>,
  val normals : Array<Float>,
  val texturecoords : Array<Array<Float>>,
  val faces : Array<Array<Short>>)

@Serializable
data class JsonModel(
  val meshes : Array<JsonMesh>)

@Serializable
data class JsonBone(
  val name : String,
  val offsetmatrix : Array<Float>,
  val weights : Array<Array<Float>>)

@Serializable
data class JsonRiggedMesh(
  val vertices : Array<Float>,
  val normals : Array<Float>,
  val texturecoords : Array<Array<Float>>,
  val faces : Array<Array<Short>>,
  val bones : Array<JsonBone>)

@Serializable
data class JsonRiggedModel(
  val meshes : Array<JsonRiggedMesh>)

class MultiMesh(
  materials : Array<Material>,
  geometries : Array<Geometry>
) : UniformProvider("multiMesh") {

  init{
    if(materials.size != geometries.size){
      throw Error("MultiMesh has ${geometries.size} geometries, but ${materials.size} materials were provided.")
    }
    val submeshes = Array<Mesh>(geometries.size) {
        i -> Mesh(materials[i], geometries[i])
    }
    addComponentsAndGatherUniforms(*submeshes)
  }

  companion object{
    fun load(gl : WebGL2RenderingContext,
             jsonModelFileUrl : String) : Array<Geometry> {
      val request = XMLHttpRequest()
      request.overrideMimeType("application/json")
      request.open("GET", jsonModelFileUrl, false)
      var geometries : Array<Geometry>? = null
      request.onloadend = {
        val json = Json{ignoreUnknownKeys=true}
        val jsonModel = json.decodeFromString(JsonModel.serializer(), request.responseText)
        geometries = Array<Geometry>(jsonModel.meshes.size) {
            i -> SubmeshGeometry(gl, jsonModel.meshes[i])
        }
        Unit
      }
      request.send()
      return geometries!!
    }
  }
}