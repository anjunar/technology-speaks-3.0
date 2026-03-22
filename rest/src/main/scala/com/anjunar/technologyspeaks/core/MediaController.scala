package com.anjunar.technologyspeaks.core

import jakarta.annotation.security.RolesAllowed
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.{GetMapping, PathVariable, RestController}

@RestController
class MediaController {

  @GetMapping(value = Array("/core/media/{id}"), produces = Array("image/jpeg", "image/png", "image/gif"))
  @RolesAllowed(Array("Anonymous", "Guest", "User", "Administrator"))
  def media(@PathVariable("id") media: Media): ResponseEntity[Array[Byte]] =
    ResponseEntity.ok()
      .header("Content-Type", media.contentType)
      .contentLength(media.data.length.toLong)
      .body(media.data)

  @GetMapping(value = Array("/core/media/{id}/thumbnail"), produces = Array("image/jpeg", "image/png", "image/gif"))
  @RolesAllowed(Array("Anonymous", "Guest", "User", "Administrator"))
  def thumbnail(@PathVariable("id") media: Media): ResponseEntity[Array[Byte]] =
    ResponseEntity.ok()
      .header("Content-Type", media.thumbnail.contentType)
      .contentLength(media.thumbnail.data.length.toLong)
      .body(media.thumbnail.data)

}
