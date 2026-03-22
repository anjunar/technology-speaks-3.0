package com.anjunar.technologyspeaks.timeline

import com.anjunar.technologyspeaks.hibernate.search.HibernateSearch
import com.anjunar.technologyspeaks.rest.types.{Data, Table}
import com.anjunar.technologyspeaks.security.{IdentityHolder, LinkBuilder}
import com.anjunar.technologyspeaks.shared.commentable.FirstComment
import jakarta.annotation.security.RolesAllowed
import org.springframework.web.bind.annotation.{GetMapping, RestController}

import scala.jdk.CollectionConverters._

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
        LinkBuilder.create(classOf[PostLikeController], "likeFirstComment")
          .withRel("like")
          .withVariable("id", row.data.id)
          .build(),
        LinkBuilder.create(classOf[PostCommentController], "update")
          .withRel("updateChildren")
          .withVariable("id", search.post.id)
          .build()
      )

      if (row.data.user == identityHolder.user) {
        row.data.addLinks(
          LinkBuilder.create(classOf[PostCommentController], "update")
            .withVariable("id", search.post.id)
            .build(),
          LinkBuilder.create(classOf[PostCommentController], "delete")
            .withVariable("id", search.post.id)
            .build()
        )
      }

      for (comment <- row.data.comments.asScala) {
        comment.addLinks(
          LinkBuilder.create(classOf[PostLikeController], "likeSecondComment")
            .withRel("like")
            .withVariable("id", comment.id)
            .build()
        )

        if (comment.user == identityHolder.user) {
          comment.addLinks(
            LinkBuilder.create(classOf[PostCommentController], "update")
              .withVariable("id", search.post.id)
              .build(),
            LinkBuilder.create(classOf[PostCommentController], "delete")
              .withVariable("id", search.post.id)
              .build()
          )
        }
      }
    }

    new Table(entities, count, Post.schema())
  }

}

object PostCommentsController {

  class CommentRow(data: FirstComment) extends Data[FirstComment](data, FirstComment.schema())

}
