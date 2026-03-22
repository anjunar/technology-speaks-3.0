package com.anjunar.technologyspeaks.documents

import com.anjunar.technologyspeaks.shared.commentable.{FirstComment, SecondComment}
import com.anjunar.technologyspeaks.shared.likeable.{Like, LikeService}
import jakarta.annotation.security.RolesAllowed
import org.springframework.web.bind.annotation.{PathVariable, PostMapping, RestController}

@RestController
class IssueLikeController(val likeService: LikeService) {

  @PostMapping(value = Array("/document/documents/document/issues/issue/{id}/like"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def likeIssue(@PathVariable("id") post: Issue): java.util.Set[Like] =
    likeService.toggle(post)

  @PostMapping(value = Array("/document/documents/document/issues/issue/comment/{id}/like"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def likeFirstComment(@PathVariable("id") comment: FirstComment): java.util.Set[Like] =
    likeService.toggle(comment)

  @PostMapping(value = Array("/document/documents/document/issues/issue/comment/comment/{id}/like"), produces = Array("application/json"))
  @RolesAllowed(Array("User", "Administrator"))
  def likeSecondComment(@PathVariable("id") comment: SecondComment): java.util.Set[Like] =
    likeService.toggle(comment)

}
