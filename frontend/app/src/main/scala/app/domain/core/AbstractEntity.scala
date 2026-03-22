package app.domain.core

import app.support.JsonModel
import jfx.core.state.{ListProperty, Property}

trait AbstractEntity[M] extends JsonModel[M] { self: M =>
  def id: Property[String]
  def modified: Property[String]
  def created: Property[String]
  def links: ListProperty[Link]
}
