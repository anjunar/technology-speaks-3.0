package com.anjunar.technologyspeaks

import com.anjunar.json.mapper.provider.DTO
import com.anjunar.technologyspeaks.core.User
import com.anjunar.technologyspeaks.rest.types.LinksContainer
import jakarta.json.bind.annotation.JsonbProperty

import scala.annotation.meta.field
import scala.beans.BeanProperty

class Application(@(JsonbProperty @field) val user: User) extends DTO with LinksContainer
