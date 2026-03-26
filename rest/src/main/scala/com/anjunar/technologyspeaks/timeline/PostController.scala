package com.anjunar.technologyspeaks.timeline

import com.anjunar.technologyspeaks.rest.EntityGraph
import com.anjunar.technologyspeaks.rest.types.Data
import com.anjunar.technologyspeaks.security.{IdentityHolder, LinkBuilder}
import jakarta.annotation.security.RolesAllowed
import org.springframework.web.bind.annotation._

@RestController
class PostController(val identityHolder: IdentityHolder) {

  @GetMapping(value = Array("/timeline/posts/post/{id}"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @EntityGraph("Post.full")
  def read(@PathVariable("id") post: Post): Data[Post] = {
    val data = new Data(post, Post.schema)

    post.addLinks(
      LinkBuilder.create(classOf[PostLikeController], "likePost")
        .withRel("like")
        .withVariable("id", post.id)
        .build()
    )

    post.addLinks(
      LinkBuilder.create(classOf[PostCommentsController], "comments")
        .withVariable("post", post.id)
        .build(),
      LinkBuilder.create(classOf[PostCommentController], "save")
        .withVariable("id", post.id)
        .build()
    )

    if (post.user == identityHolder.user) {
      post.addLinks(
        LinkBuilder.create(classOf[PostController], "read")
          .withVariable("id", post.id)
          .build(),
        LinkBuilder.create(classOf[PostController], "delete")
          .withVariable("id", post.id)
          .build()
      )
    }

    data
  }

  @PostMapping(value = Array("/timeline/posts/post"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @EntityGraph("Post.full")
  def save(@RequestBody post: Post): Data[Post] = {
    post.user = identityHolder.user
    post.persist()
    val data = new Data(post, Post.schema)

    post.addLinks(
      LinkBuilder.create(classOf[PostLikeController], "likePost")
        .withRel("like")
        .withVariable("id", post.id)
        .build()
    )

    post.addLinks(
      LinkBuilder.create(classOf[PostCommentsController], "comments")
        .withVariable("post", post.id)
        .build(),
      LinkBuilder.create(classOf[PostCommentController], "save")
        .withVariable("id", post.id)
        .build()
    )

    if (post.user == identityHolder.user) {
      post.addLinks(
        LinkBuilder.create(classOf[PostController], "read")
          .withVariable("id", post.id)
          .build(),
        LinkBuilder.create(classOf[PostController], "delete")
          .withVariable("id", post.id)
          .build()
      )
    }

    data
  }

  @PutMapping(value = Array("/timeline/posts/post"), produces = Array("application/json"), consumes = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  @EntityGraph("Post.full")
  def update(@RequestBody post: Post): Data[Post] = {
    val data = new Data(post.merge(), Post.schema)

    post.addLinks(
      LinkBuilder.create(classOf[PostLikeController], "likePost")
        .withRel("like")
        .withVariable("id", post.id)
        .build()
    )

    post.addLinks(
      LinkBuilder.create(classOf[PostCommentsController], "comments")
        .withVariable("post", post.id)
        .build(),
      LinkBuilder.create(classOf[PostCommentController], "save")
        .withVariable("id", post.id)
        .build()
    )

    if (post.user == identityHolder.user) {
      post.addLinks(
        LinkBuilder.create(classOf[PostController], "read")
          .withVariable("id", post.id)
          .build(),
        LinkBuilder.create(classOf[PostController], "update")
          .build(),
        LinkBuilder.create(classOf[PostController], "delete")
          .withVariable("id", post.id)
          .build()
      )
    }

    data
  }

  @DeleteMapping(value = Array("/timeline/posts/post/{id}"))
  @RolesAllowed(Array("User", "Administrator"))
  @EntityGraph("Post.full")
  def delete(@PathVariable("id") post: Post): Unit =
    post.remove()

}
