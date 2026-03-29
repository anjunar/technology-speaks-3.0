package com.anjunar.technologyspeaks.followers

import com.anjunar.technologyspeaks.core.SchemaHateoas
import com.anjunar.technologyspeaks.rest.EntityGraph
import com.anjunar.technologyspeaks.rest.types.Data
import com.anjunar.technologyspeaks.security.LinkBuilder
import jakarta.annotation.security.RolesAllowed
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.{DeleteMapping, GetMapping, PathVariable, PostMapping, PutMapping, RequestBody, RestController}

@RestController
class RelationShipController {

  @GetMapping(value = Array("/followers/relationships/relationship/{id}"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @EntityGraph("RelationShip.full")
  def read(@PathVariable("id") entity: RelationShip): Data[RelationShip] = {
    entity.addLinks(
      LinkBuilder.create[RelationShipController](_.update(null))
        .build(),
      LinkBuilder.create[RelationShipController](_.delete(null))
        .build()
    )

    new Data(entity, SchemaHateoas.enhance(entity, RelationShip.schema))
  }

  @PostMapping(value = Array("/followers/relationships/relationship"), consumes = Array("application/json"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @EntityGraph("RelationShip.full")
  def save(@RequestBody entity: RelationShip): Data[RelationShip] = {
    entity.persist()

    entity.addLinks(
      LinkBuilder.create[RelationShipController](_.update(null))
        .build(),
      LinkBuilder.create[RelationShipController](_.delete(null))
        .build()
    )

    new Data(entity, SchemaHateoas.enhance(entity, RelationShip.schema))
  }

  @PutMapping(value = Array("/followers/relationships/relationship"), consumes = Array("application/json"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @EntityGraph("RelationShip.full")
  def update(@RequestBody entity: RelationShip): Data[RelationShip] = {
    val managed = entity.merge()
    managed.addLinks(
      LinkBuilder.create[RelationShipController](_.update(null))
        .build(),
      LinkBuilder.create[RelationShipController](_.delete(null))
        .build()
    )

    new Data(managed, SchemaHateoas.enhance(managed, RelationShip.schema))
  }

  @DeleteMapping(value = Array("/followers/relationships/relationship"), consumes = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @EntityGraph("RelationShip.full")
  def delete(@RequestBody entity: RelationShip): ResponseEntity[Void] = {
    entity.remove()
    ResponseEntity.ok().build()
  }

}
