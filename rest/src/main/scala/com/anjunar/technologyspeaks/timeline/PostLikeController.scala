package com.anjunar.technologyspeaks.timeline

import com.anjunar.technologyspeaks.shared.commentable.{FirstComment, SecondComment}
import com.anjunar.technologyspeaks.shared.likeable.{Like, LikeService}
import jakarta.annotation.security.RolesAllowed
import org.springframework.web.bind.annotation.{PathVariable, PostMapping, RestController}

@RestController
class PostLikeController(val likeService: LikeService) {

  @PostMapping(value = Array("/timeline/posts/post/{id}/like"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def likePost(@PathVariable("id") post: Post): java.util.Set[Like] =
    likeService.toggle(post)

  @PostMapping(value = Array("/timeline/posts/post/comment/{id}/like"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def likeFirstComment(@PathVariable("id") comment: FirstComment): java.util.Set[Like] =
    likeService.toggle(comment)

  @PostMapping(value = Array("/timeline/posts/post/comment/comment/{id}/like"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def likeSecondComment(@PathVariable("id") comment: SecondComment): java.util.Set[Like] =
    likeService.toggle(comment)

}
