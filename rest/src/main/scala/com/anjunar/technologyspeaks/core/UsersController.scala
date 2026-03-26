package com.anjunar.technologyspeaks.core

import com.anjunar.technologyspeaks.hibernate.search.HibernateSearch
import com.anjunar.technologyspeaks.rest.EntityGraph
import com.anjunar.technologyspeaks.rest.types.{Data, Table}
import com.anjunar.technologyspeaks.security.LinkBuilder
import jakarta.annotation.security.RolesAllowed
import jakarta.json.bind.annotation.{JsonbProperty, JsonbSubtype}
import org.springframework.web.bind.annotation.{GetMapping, RestController}

import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.jdk.CollectionConverters.*

@RestController
class UsersController(val query: HibernateSearch) {

  @GetMapping(value = Array("/core/users"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @EntityGraph("User.full")
  def list(search: UserSearch): Table[UsersController.UserRow] = {
    val searchContext = query.searchContext(search)

    val entities = query.entities(
      search.index,
      search.limit,
      classOf[User],
      classOf[UsersController.UserRow],
      searchContext,
      (criteriaQuery, root, expressions, builder) =>
        criteriaQuery.select(
          builder.construct(
            classOf[UsersController.UserRow],
            root,
            expressions.stream().findFirst().orElse(builder.literal(java.lang.Double.valueOf(1.0d)))
          )
        )
    )

    val count = query.count(classOf[User], searchContext)

    for (user <- entities.asScala) {
      user.data.addLinks(
        LinkBuilder.create[UserController](_.read(user.data))
          .build()
      )
    }

    new Table(entities, count, User.schema)
  }

}

object UsersController {

  @JsonbSubtype(alias = "Data", `type` = classOf[Data[?]])
  class UserRow(data: User, @(JsonbProperty @field) val score: Double) extends Data[User](data, User.schema)

}
