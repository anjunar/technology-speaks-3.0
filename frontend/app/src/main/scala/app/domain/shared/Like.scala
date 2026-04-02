package app.domain.shared

import app.domain.core.User
import app.support.JsonModel
import jfx.core.meta.Meta

import scala.scalajs.js

class Like(
  var user: User = new User()
) extends JsonModel[Like] {


}

object Like {

}
