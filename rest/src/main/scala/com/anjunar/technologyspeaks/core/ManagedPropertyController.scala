package com.anjunar.technologyspeaks.core

import org.springframework.web.bind.annotation.{GetMapping, PathVariable, PutMapping, RestController}

@RestController
class ManagedPropertyController {

  @GetMapping(value = Array("/core/properties/property/{id}"), produces = Array("application/json"))
  def read(@PathVariable("id") managedProperty: ManagedProperty): ManagedProperty =
    managedProperty

  @PutMapping(value = Array("/core/properties/property"), produces = Array("application/json"), consumes = Array("application/json"))
  def update(managedProperty: ManagedProperty): ManagedProperty =
    managedProperty

}
