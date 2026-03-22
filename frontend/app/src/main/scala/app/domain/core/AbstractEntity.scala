package app.domain.core

import app.support.JsonModel
import jfx.core.state.{ListProperty, Property}

import java.util.UUID

trait AbstractEntity[M] extends JsonModel[M] { self: M =>
  def id: Property[UUID]
  def modified: Property[String]
  def created: Property[String]
  def links: ListProperty[Link]
}
