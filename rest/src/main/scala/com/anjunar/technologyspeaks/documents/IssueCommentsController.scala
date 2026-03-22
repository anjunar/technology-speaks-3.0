package com.anjunar.technologyspeaks.documents

import com.anjunar.technologyspeaks.hibernate.search.HibernateSearch
import com.anjunar.technologyspeaks.rest.types.{Data, Table}
import com.anjunar.technologyspeaks.security.{IdentityHolder, LinkBuilder}
import com.anjunar.technologyspeaks.shared.commentable.FirstComment
import jakarta.annotation.security.RolesAllowed
import org.springframework.web.bind.annotation.{GetMapping, RestController}

import scala.jdk.CollectionConverters._

@RestController
class IssueCommentsController(val query: HibernateSearch, val identityHolder: IdentityHolder) {

  @GetMapping(value = Array("/document/documents/document/issues/issue/{issue}/comments"), produces = Array("application/json"))
  @RolesAllowed(Array("Anonymous", "Guest", "User", "Administrator"))
  def comments(search: IssueCommentSearch): Table[IssueCommentsController.CommentRow] = {
    val searchContext = query.searchContext(search)

    val entities = query.entities(
      search.index,
      search.limit,
      classOf[FirstComment],
      classOf[IssueCommentsController.CommentRow],
      searchContext,
      (criteriaQuery, root, expressions, builder) =>
        criteriaQuery.select(builder.construct(classOf[IssueCommentsController.CommentRow], root))
    )

    val count = query.count(classOf[FirstComment], searchContext)

    for (row <- entities.asScala) {
      row.data.addLinks(
        LinkBuilder.create(classOf[IssueLikeController], "likeFirstComment")
          .withRel("like")
          .withVariable("id", row.data.id)
          .build(),
        LinkBuilder.create(classOf[IssueCommentController], "update")
          .withRel("updateChildren")
          .withVariable("id", search.issue.id)
          .build()
      )

      if (row.data.user == identityHolder.user) {
        row.data.addLinks(
          LinkBuilder.create(classOf[IssueCommentController], "update")
            .withVariable("id", search.issue.id)
            .build(),
          LinkBuilder.create(classOf[IssueCommentController], "delete")
            .withVariable("id", search.issue.id)
            .build()
        )
      }

      for (comment <- row.data.comments.asScala) {
        comment.addLinks(
          LinkBuilder.create(classOf[IssueLikeController], "likeSecondComment")
            .withRel("like")
            .withVariable("id", comment.id)
            .build()
        )

        if (comment.user == identityHolder.user) {
          comment.addLinks(
            LinkBuilder.create(classOf[IssueCommentController], "update")
              .withVariable("id", search.issue.id)
              .build(),
            LinkBuilder.create(classOf[IssueCommentController], "delete")
              .withVariable("id", search.issue.id)
              .build()
          )
        }
      }
    }

    new Table(entities, count, Issue.schema())
  }

}

object IssueCommentsController {

  class CommentRow(data: FirstComment) extends Data[FirstComment](data, FirstComment.schema())

}
