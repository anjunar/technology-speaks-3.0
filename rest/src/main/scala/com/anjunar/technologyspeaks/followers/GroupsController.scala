package com.anjunar.technologyspeaks.followers

import com.anjunar.technologyspeaks.hibernate.search.HibernateSearch
import com.anjunar.technologyspeaks.rest.types.{Data, Table}
import com.anjunar.technologyspeaks.security.{IdentityHolder, LinkBuilder}
import jakarta.annotation.security.RolesAllowed
import jakarta.json.bind.annotation.{JsonbProperty, JsonbSubtype}
import org.springframework.web.bind.annotation.{GetMapping, RestController}

import scala.annotation.meta.field
import scala.jdk.CollectionConverters.*

@RestController
class GroupsController(val query: HibernateSearch, val identityHolder: IdentityHolder) {

  @GetMapping(value = Array("/followers/groups"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def list(search: GroupSearch): Table[GroupsController.GroupRow] = {
    search.user = identityHolder.user

    val searchContext = query.searchContext(search)

    val entities = query.entities(
      search.index,
      search.limit,
      classOf[Group],
      classOf[GroupsController.GroupRow],
      searchContext,
      (criteriaQuery, root, expressions, builder) =>
        criteriaQuery.select(builder.construct(classOf[GroupsController.GroupRow], root))
    )

    val count = query.count(classOf[Group], searchContext)

    for (entity <- entities.asScala) {
      entity.data.addLinks(
        LinkBuilder.create[GroupController](_.read(entity.data))
          .build()
      )
    }

    new Table(entities, count, Group.schema)
  }

}

object GroupsController {

  @JsonbSubtype(alias = "Data", `type` = classOf[Data[?]])
  class GroupRow(@(JsonbProperty @field) data: Group) extends Data[Group](data, Group.schema)

}
