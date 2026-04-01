package app.support

import jfx.json.JsonMapper

object AppJson {

  val registry: AppJsonRegistry = new AppJsonRegistry

  val mapper: JsonMapper = registry.mapper
}
