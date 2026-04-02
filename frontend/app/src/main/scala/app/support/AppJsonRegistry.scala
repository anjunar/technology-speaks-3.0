package app.support

import app.domain.Application
import app.domain.core.*
import app.domain.documents.*
import app.domain.followers.*
import app.domain.security.*
import app.domain.shared.*
import app.domain.timeline.*
import jfx.domain.{Media, Thumbnail}
import jfx.form.ErrorResponse
import jfx.json.JsonMapper

import scala.scalajs.js
import scala.scalajs.js.Dynamic
import scala.scalajs.js.JSConverters.*

class AppJsonRegistry {

  val mapper = new JsonMapper()

}
