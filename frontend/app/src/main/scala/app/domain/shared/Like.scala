package app.domain.shared

import app.domain.core.User
import app.support.JsonModel
import com.anjunar.scala.enterprise.macros.{PropertyMacros, PropertyAccess}

import scala.scalajs.js

class Like(
  var user: User = new User()
) extends JsonModel[Like] {

  override def properties: Seq[PropertyAccess[Like, ?]] = Like.properties
}

object Like {
  val properties: Seq[PropertyAccess[Like, ?]] = PropertyMacros.describeProperties[Like]
}
