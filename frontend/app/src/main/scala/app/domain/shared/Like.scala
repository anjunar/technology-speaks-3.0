package app.domain.shared

import app.domain.core.User
import app.support.JsonModel
import jfx.core.macros.property
import jfx.core.state.PropertyAccess

import scala.scalajs.js

class Like(
  var user: User = new User()
) extends JsonModel[Like] {

  override def properties: js.Array[PropertyAccess[Like, ?]] =
    Like.properties
}

object Like {
  val properties: js.Array[PropertyAccess[Like, ?]] = js.Array(
    property(_.user)
  )
}
