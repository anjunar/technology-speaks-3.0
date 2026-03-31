package jfx.core.meta

import scala.collection.mutable

object ClassLoader {

  val classes : mutable.Map[Class[?], ( () => Any, String )] = mutable.HashMap()

}
