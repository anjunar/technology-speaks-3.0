package com.anjunar.technologyspeaks.documents

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
class DocumentsController(val query: HibernateSearch) {

  @GetMapping(value = Array("/document/documents"), produces = Array("application/json"))
  @RolesAllowed(Array("Anonymous", "Guest", "User", "Administrator"))
  @EntityGraph("Document.full")
  def list(search: DocumentSearch): Table[DocumentsController.DocumentRow] = {
    val searchContext = query.searchContext(search)

    val entities = query.entities(
      search.index,
      search.limit,
      classOf[Document],
      classOf[DocumentsController.DocumentRow],
      searchContext,
      (criteriaQuery, root, expressions, builder) =>
        criteriaQuery.select(
          builder.construct(
            classOf[DocumentsController.DocumentRow],
            root,
            expressions.stream().findFirst().orElse(builder.literal(java.lang.Double.valueOf(1.0d)))
          )
        )
    )

    val count = query.count(classOf[Document], searchContext)

    for (entity <- entities.asScala) {
      entity.data.addLinks(
        LinkBuilder.create(classOf[DocumentController], "read")
          .withVariable("id", entity.data.id)
          .build()
      )
    }

    val table = new Table(entities, count, Document.schema())

    table.addLinks(
      LinkBuilder.create(classOf[DocumentController], "create")
        .build()
    )

    table
  }

}

object DocumentsController {

  @JsonbSubtype(alias = "Data", `type` = classOf[Data[?]])
  class DocumentRow(data: Document, @(JsonbProperty @field) val score: Double) extends Data[Document](data, Document.schema())

}
