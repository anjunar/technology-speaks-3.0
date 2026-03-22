package com.anjunar.json.mapper.schema

trait SchemaProvider {

  def schema(): EntitySchema[?]

}
