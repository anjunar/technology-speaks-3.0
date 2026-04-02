package com.anjunar.technologyspeaks.core

import com.anjunar.technologyspeaks.rest.EntityManagerProvider
import com.anjunar.technologyspeaks.security.LinkBuilder
import jakarta.persistence.EntityManager
import org.springframework.web.bind.annotation.{GetMapping, PathVariable, PutMapping, RequestBody, RestController}

@RestController
class ManagedPropertyController(val entityManager : EntityManager) extends EntityManagerProvider {

  @GetMapping(value = Array("/core/properties/property/{id}"), produces = Array("application/json"))
  def read(@PathVariable("id") managedProperty: ManagedProperty): ManagedProperty =
    enrich(managedProperty)

  @PutMapping(value = Array("/core/properties/property"), produces = Array("application/json"), consumes = Array("application/json"))
  def update(@RequestBody managedProperty: ManagedProperty): ManagedProperty =
    enrich(managedProperty.merge())

  private def enrich(managedProperty: ManagedProperty): ManagedProperty = {
    managedProperty.addLinks(
      LinkBuilder.create[ManagedPropertyController](_.read(new ManagedProperty("")))
        .withRel("self")
        .withVariable("id", managedProperty.id)
        .build(),
      LinkBuilder.create[ManagedPropertyController](_.update(new ManagedProperty("")))
        .build()
    )

    managedProperty
  }

}
