package com.anjunar.technologyspeaks.followers

import com.anjunar.technologyspeaks.core.UserController
import com.anjunar.technologyspeaks.hibernate.search.HibernateSearch
import com.anjunar.technologyspeaks.rest.EntityGraph
import com.anjunar.technologyspeaks.rest.types.{Data, Table}
import com.anjunar.technologyspeaks.security.{IdentityHolder, LinkBuilder}
import jakarta.annotation.security.RolesAllowed
import jakarta.json.bind.annotation.{JsonbProperty, JsonbSubtype}
import org.springframework.web.bind.annotation.{GetMapping, RestController}

import scala.annotation.meta.field
import scala.jdk.CollectionConverters.*

@RestController
class FollowersController(val query: HibernateSearch, val identityHolder: IdentityHolder) {

  @GetMapping(value = Array("/followers/relationships"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @EntityGraph("RelationShip.full")
  def list(search: RelationShipSearch): Table[FollowersController.RelationShipRow] = {
    search.user = identityHolder.user

    val searchContext = query.searchContext(search)

    val entities = query.entities(
      search.index,
      search.limit,
      classOf[RelationShip],
      classOf[FollowersController.RelationShipRow],
      searchContext,
      (criteriaQuery, root, expressions, builder) =>
        criteriaQuery.select(builder.construct(classOf[FollowersController.RelationShipRow], root))
    )

    val count = query.count(classOf[RelationShip], searchContext)

    for (entity <- entities.asScala) {
      entity.data.addLinks(
        LinkBuilder.create(classOf[UserController], "read")
          .withVariable("id", entity.data.follower.id)
          .build()
      )
    }

    new Table(entities, count, RelationShip.schema())
  }

}

object FollowersController {

  @JsonbSubtype(alias = "Data", `type` = classOf[Data[?]])
  class RelationShipRow(@(JsonbProperty @field) data: RelationShip) extends Data[RelationShip](data, RelationShip.schema())

}
