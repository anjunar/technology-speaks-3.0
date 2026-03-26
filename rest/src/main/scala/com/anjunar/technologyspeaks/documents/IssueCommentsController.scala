package com.anjunar.technologyspeaks.documents

import com.anjunar.technologyspeaks.hibernate.search.HibernateSearch
import com.anjunar.technologyspeaks.rest.types.{Data, Table}
import com.anjunar.technologyspeaks.security.{IdentityHolder, LinkBuilder}
import com.anjunar.technologyspeaks.shared.commentable.FirstComment
import jakarta.annotation.security.RolesAllowed
import jakarta.json.bind.annotation.JsonbSubtype
import org.springframework.web.bind.annotation.{GetMapping, RestController}

import scala.jdk.CollectionConverters.*

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
        LinkBuilder.create[IssueLikeController](_.likeFirstComment(row.data))
          .withRel("like")
          .build(),
        LinkBuilder.create[IssueCommentController](_.update(search.issue, null))
          .withRel("updateChildren")
          .build()
      )

      if (row.data.user == identityHolder.user) {
        row.data.addLinks(
          LinkBuilder.create[IssueCommentController](_.update(search.issue, null))
            .build(),
          LinkBuilder.create[IssueCommentController](_.delete(search.issue, null))
            .build()
        )
      }

      for (comment <- row.data.comments.asScala) {
        comment.addLinks(
          LinkBuilder.create[IssueLikeController](_.likeSecondComment(comment))
            .withRel("like")
            .build()
        )

        if (comment.user == identityHolder.user) {
          comment.addLinks(
            LinkBuilder.create[IssueCommentController](_.update(search.issue, null))
              .build(),
            LinkBuilder.create[IssueCommentController](_.delete(search.issue, null))
              .build()
          )
        }
      }
    }

    new Table(entities, count, Issue.schema)
  }

}

object IssueCommentsController {

  @JsonbSubtype(alias = "Data", `type` = classOf[Data[?]])
  class CommentRow(data: FirstComment) extends Data[FirstComment](data, FirstComment.schema)

}
