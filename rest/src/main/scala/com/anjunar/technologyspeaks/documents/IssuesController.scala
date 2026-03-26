package com.anjunar.technologyspeaks.documents

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
class IssuesController(val query: HibernateSearch, val identityHolder: IdentityHolder) {

  @GetMapping(value = Array("/document/documents/document/{id}/issues"), produces = Array("application/json"))
  @RolesAllowed(Array("Anonymous", "Guest", "User", "Administrator"))
  @EntityGraph("Issue.full")
  def list(search: IssueSearch): Table[IssuesController.IssueRow] = {
    val searchContext = query.searchContext(search)

    val entities = query.entities(
      search.index,
      search.limit,
      classOf[Issue],
      classOf[IssuesController.IssueRow],
      searchContext,
      (criteriaQuery, root, expressions, builder) =>
        criteriaQuery.select(builder.construct(classOf[IssuesController.IssueRow], root))
    )

    val count = query.count(classOf[Issue], searchContext)

    for (entity <- entities.asScala) {
      entity.data.addLinks(
        LinkBuilder.create[IssueController](_.read(entity.data.document, entity.data))
          .build()
      )

      if (identityHolder.user == entity.data.user) {
        entity.data.addLinks(
          LinkBuilder.create[IssueController](_.delete(null))
            .build()
        )
      }
    }

    new Table(entities, count, Issue.schema)
  }

}

object IssuesController {

  @JsonbSubtype(alias = "Data", `type` = classOf[Data[?]])
  class IssueRow(@(JsonbProperty @field) data: Issue) extends Data[Issue](data, Issue.schema)

}
