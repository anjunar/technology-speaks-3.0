package com.anjunar.technologyspeaks.curation

import com.anjunar.technologyspeaks.security.IdentityHolder
import jakarta.annotation.security.RolesAllowed
import org.springframework.web.bind.annotation.{GetMapping, RestController}

@RestController
class CurationController(val identityHolder: IdentityHolder) {

  @GetMapping(value = Array("/curation/space"), produces = Array("application/json"))
  @RolesAllowed(Array("Anonymous", "Guest", "User", "Administrator"))
  def space(): CurationSpace =
    new CurationSpace("Verdichtungsraum", "Resonanzen werden hier geprueft, geordnet und gezielt dem Wissensraum zugefuehrt.")
}

class CurationSpace(var title: String = "", var subtitle: String = "")
