package com.anjunar.json.mapper.schema

object RuntimeClassResolver {

  def resolve(name: String): Class[?] =
    name match {
      case "scala.Int"     => java.lang.Integer.TYPE
      case "scala.Long"    => java.lang.Long.TYPE
      case "scala.Double"  => java.lang.Double.TYPE
      case "scala.Float"   => java.lang.Float.TYPE
      case "scala.Boolean" => java.lang.Boolean.TYPE
      case "scala.Byte"    => java.lang.Byte.TYPE
      case "scala.Short"   => java.lang.Short.TYPE
      case "scala.Char"    => java.lang.Character.TYPE
      case "scala.Unit"    => java.lang.Void.TYPE
      case other           => Class.forName(other)
    }
}