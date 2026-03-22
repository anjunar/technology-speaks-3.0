package app.domain.shared

import app.domain.core.{Link, User}
import jfx.core.state.{ListProperty, Property}

trait OwnerProvider {
  def modified: Property[String]
  def created: Property[String]
  def user: Property[User | Null]
  def links: ListProperty[Link]
}
