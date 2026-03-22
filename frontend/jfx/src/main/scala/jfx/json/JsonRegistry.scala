package jfx.json

import scala.scalajs.js

trait JsonRegistry {

  val classes : js.Map[String, () => Any]

  def normalizeFieldName(name: String): String =
    name match {
      case "$links" => "links"
      case other    => other
    }

  def serializeFieldName(name: String): String =
    name match {
      case "links" => "$links"
      case other   => other
    }

}
