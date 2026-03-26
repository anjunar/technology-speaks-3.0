package com.anjunar.technologyspeaks.documents

import com.anjunar.technologyspeaks.hibernate.search.annotations.RestPredicate
import com.anjunar.technologyspeaks.hibernate.search.{AbstractSearch, Context, PredicateProvider}
import com.anjunar.technologyspeaks.shared.commentable.FirstComment
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.criteria.Predicate
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PathVariable

import java.util.UUID
import scala.beans.BeanProperty

class IssueCommentSearch(
  issueValue: Issue = null,
  sort: java.util.List[String] = new java.util.ArrayList[String](),
  index: Int = 0,
  limit: Int = 5
) extends AbstractSearch(sort, index, limit) {

  @JsonbProperty
  @PathVariable("issue")
  @RestPredicate(classOf[IssueCommentSearch.IssuePredicate])
    val issue: Issue = issueValue

}

object IssueCommentSearch {

  @Component
  class IssuePredicate extends PredicateProvider[Issue, FirstComment] {
    override def build(context: Context[Issue, FirstComment]): Unit = {
      val issueQuery = context.query.subquery(classOf[FirstComment])
      val issueFrom = issueQuery.from(classOf[Issue])
      val commentsJoin = issueFrom.join[Issue, FirstComment]("comments")

      val condition: Predicate = context.builder.equal(issueFrom.get[UUID]("id"), context.value.id)

      issueQuery
        .select(commentsJoin)
        .where(Array(condition)*)

      context.predicates.add(context.root.in(issueQuery))
    }
  }

}
