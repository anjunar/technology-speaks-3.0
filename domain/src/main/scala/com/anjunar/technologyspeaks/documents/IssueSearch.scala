package com.anjunar.technologyspeaks.documents

import com.anjunar.technologyspeaks.hibernate.search.annotations.RestPredicate
import com.anjunar.technologyspeaks.hibernate.search.{AbstractSearch, Context, PredicateProvider}
import org.springframework.stereotype.Component

import java.util.UUID
import scala.beans.BeanProperty

class IssueSearch(
  idValue: Document = null,
  sort: java.util.List[String] = new java.util.ArrayList[String](),
  index: Int = 0,
  limit: Int = 5
) extends AbstractSearch(sort, index, limit) {

  @RestPredicate(classOf[IssueSearch.DocumentPredicate])
  @BeanProperty
  val id: Document = idValue

}

object IssueSearch {

  @Component
  class DocumentPredicate extends PredicateProvider[Document, Issue] {
    override def build(context: Context[Document, Issue]): Unit = {
      context.predicates.add(
        context.builder.equal(
          context.root.get[Document]("document").get[UUID]("id"),
          context.value.id
        )
      )
    }
  }

}
