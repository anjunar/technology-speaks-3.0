package com.anjunar.technologyspeaks.timeline

import com.anjunar.technologyspeaks.core.SchemaHateoas
import com.anjunar.technologyspeaks.hibernate.search.HibernateSearch
import com.anjunar.technologyspeaks.rest.EntityGraph
import com.anjunar.technologyspeaks.rest.types.{Data, Table}
import com.anjunar.technologyspeaks.security.{IdentityHolder, LinkBuilder}
import jakarta.annotation.security.RolesAllowed
import jakarta.json.bind.annotation.JsonbSubtype
import org.springframework.web.bind.annotation.{GetMapping, RestController}

import scala.jdk.CollectionConverters.*

@RestController
class PostsController(val query: HibernateSearch, val identityHolder: IdentityHolder) {

  @GetMapping(value = Array("/timeline/posts"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @EntityGraph("Post.full")
  def list(search: PostSearch): Table[PostsController.PostRow] = {
    val searchContext = query.searchContext(search)

    val entities = query.entities(
      search.index,
      search.limit,
      classOf[Post],
      classOf[PostsController.PostRow],
      searchContext,
      (criteriaQuery, root, expressions, builder) =>
        criteriaQuery.select(builder.construct(classOf[PostsController.PostRow], root))
    )

    val count = query.count(classOf[Post], searchContext)

    for (post <- entities.asScala) {
      post.data.addLinks(
        LinkBuilder.create[PostLikeController](_.likePost(post.data))
          .withRel("like")
          .build()
      )

      if (identityHolder.user == post.data.user) {
        post.data.addLinks(
          LinkBuilder.create[PostController](_.read(post.data))
            .build()
        )
      }
    }

    new Table(entities, count, SchemaHateoas.enhance(entities.asScala.headOption.map(_.data).orNull, Post.schema))
  }

}

object PostsController {

  @JsonbSubtype(alias = "Data", `type` = classOf[Data[?]])
  class PostRow(data: Post) extends Data[Post](data, SchemaHateoas.enhance(data, Post.schema))

}
