package jfx.json

import scala.annotation.StaticAnnotation

/**
 * Annotation um den JSON @type Namen für eine Klasse festzulegen.
 * Wird verwendet beim Serialisieren und Deserialisieren.
 */
final case class JsonType(value: String) extends StaticAnnotation
