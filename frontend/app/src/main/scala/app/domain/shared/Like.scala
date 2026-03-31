package app.domain.shared

import app.domain.core.User
import app.support.JsonModel
import jfx.core.meta.Meta

import scala.scalajs.js

class Like(
  var user: User = new User()
) extends JsonModel[Like] {

  override def meta: Meta[Like] = Like.meta
}

object Like {
  val meta: Meta[Like] = Meta(() => new Like())
}
