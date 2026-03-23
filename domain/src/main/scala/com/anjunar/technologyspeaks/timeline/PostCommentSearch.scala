package com.anjunar.technologyspeaks.timeline

import com.anjunar.technologyspeaks.hibernate.search.annotations.RestPredicate
import com.anjunar.technologyspeaks.hibernate.search.{AbstractSearch, Context, PredicateProvider}
import com.anjunar.technologyspeaks.shared.commentable.FirstComment
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.criteria.Predicate
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PathVariable

import java.util.UUID
import scala.annotation.meta.field
import scala.beans.BeanProperty

class PostCommentSearch(
                         @(JsonbProperty @field)
                         @(RestPredicate @field)(classOf[PostCommentSearch.PostPredicate])
                         @BeanProperty
                         val post: Post = null,
                         sort: java.util.List[String],
                         index: Int = 0,
                         limit: Int = 5
                       ) extends AbstractSearch(sort, index, limit)

object PostCommentSearch {

  @Component
  class PostPredicate extends PredicateProvider[Post, FirstComment] {
    override def build(context: Context[Post, FirstComment]): Unit = {
      val postQuery = context.query.subquery(classOf[FirstComment])
      val postFrom = postQuery.from(classOf[Post])
      val commentsJoin = postFrom.join[Post, FirstComment]("comments")

      val condition: Predicate = context.builder.equal(postFrom.get[UUID]("id"), context.value.id)

      postQuery
        .select(commentsJoin)
        .where(Array(condition) *)

      context.predicates.add(context.root.in(postQuery))
    }
  }

}
