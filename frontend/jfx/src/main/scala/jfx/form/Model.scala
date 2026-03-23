package jfx.form

import jfx.core.state.PropertyAccess

import scala.scalajs.js

trait Model[M] {
  
  this : M =>
  
    def properties : js.Array[PropertyAccess[M, ?]]

    def findPropertyOption[V](name: String): Option[V] =
      properties
        .find(_.name == name)
        .flatMap(_.get(this))
        .map(_.asInstanceOf[V])

    def findProperty[V](name : String) : V =
      findPropertyOption[V](name).orNull.asInstanceOf[V]

}
