package com.anjunar.technologyspeaks.timeline

import com.anjunar.technologyspeaks.core.SchemaHateoas
import com.anjunar.technologyspeaks.hibernate.search.HibernateSearch
import com.anjunar.technologyspeaks.rest.types.{Data, Table}
import com.anjunar.technologyspeaks.security.{IdentityHolder, LinkBuilder}
import com.anjunar.technologyspeaks.shared.commentable.FirstComment
import jakarta.annotation.security.RolesAllowed
import jakarta.json.bind.annotation.JsonbSubtype
import org.springframework.web.bind.annotation.{GetMapping, RestController}

import scala.jdk.CollectionConverters.*

@RestController
class PostCommentsController(val query: HibernateSearch, val identityHolder: IdentityHolder) {

  @GetMapping(value = Array("/timeline/posts/post/{post}/comments"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def comments(search: PostCommentSearch): Table[PostCommentsController.CommentRow] = {
    val searchContext = query.searchContext(search)

    val entities = query.entities(
      search.index,
      search.limit,
      classOf[FirstComment],
      classOf[PostCommentsController.CommentRow],
      searchContext,
      (criteriaQuery, root, expressions, builder) =>
        criteriaQuery.select(builder.construct(classOf[PostCommentsController.CommentRow], root))
    )

    val count = query.count(classOf[FirstComment], searchContext)

    for (row <- entities.asScala) {
      row.data.addLinks(
        LinkBuilder.create[PostLikeController](_.likeFirstComment(row.data))
          .withRel("like")
          .build(),
        LinkBuilder.create[PostCommentController](_.update(search.post, null))
          .withRel("updateChildren")
          .build()
      )

      if (row.data.user == identityHolder.user) {
        row.data.addLinks(
          LinkBuilder.create[PostCommentController](_.update(search.post, null))
            .build(),
          LinkBuilder.create[PostCommentController](_.delete(search.post, null))
            .build()
        )
      }

      for (comment <- row.data.comments.asScala) {
        comment.addLinks(
          LinkBuilder.create[PostLikeController](_.likeSecondComment(comment))
            .withRel("like")
            .build()
        )

        if (comment.user == identityHolder.user) {
          comment.addLinks(
            LinkBuilder.create[PostCommentController](_.update(search.post, null))
              .build(),
            LinkBuilder.create[PostCommentController](_.delete(search.post, null))
              .build()
          )
        }
      }
    }

    new Table(entities, count, SchemaHateoas.enhance(entities.asScala.headOption.map(_.data).orNull, Post.schema))
  }

}

object PostCommentsController {

  @JsonbSubtype(alias = "Data", `type` = classOf[Data[?]])
  class CommentRow(data: FirstComment) extends Data[FirstComment](data, SchemaHateoas.enhance(data, FirstComment.schema))

}
