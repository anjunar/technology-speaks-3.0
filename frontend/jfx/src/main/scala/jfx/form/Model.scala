package jfx.form

import com.anjunar.scala.enterprise.macros.PropertyAccess
import jfx.core.meta.Meta

import scala.scalajs.js

trait Model[M] {

  this : M =>

    def meta : Meta[M]

    def findPropertyAccessOption(name: String): Option[PropertyAccess[M, ?]] =
      meta.properties.find(_.name == name)

    def findPropertyOption[V](name: String): Option[V] =
      findPropertyAccessOption(name)
        .map(_.get(this).asInstanceOf[V])

    def findProperty[V](name : String) : V =
      findPropertyOption[V](name).orNull.asInstanceOf[V]

}
