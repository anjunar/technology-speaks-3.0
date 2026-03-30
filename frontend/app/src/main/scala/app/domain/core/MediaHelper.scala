package app.domain.core

import jfx.core.state.Property
import jfx.domain.{Thumbnail, Media}

import java.util.UUID

object MediaHelper {

  def dataUrl(value : Thumbnail): String =
    s"data:${value.contentType.get};base64,${value.data.get}"

  def mediaLink(value : Thumbnail): String =
    s"/service/core/media/${value.id.get.toString}"

  def thumbnailLink(value : Thumbnail): String =
    s"/service/core/media/${value.id.get.toString}/thumbnail"

  def mediaLink(value: Media): String =
    s"/service/core/media/${value.id.get.toString}"

  def thumbnailLink(value: Media): String =
    s"/service/core/media/${value.id.get.toString}/thumbnail"

}
