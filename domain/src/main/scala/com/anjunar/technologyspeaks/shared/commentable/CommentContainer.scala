package com.anjunar.technologyspeaks.shared.commentable

object CommentContainer {

  trait Interface {
    def comments: java.util.Set[FirstComment]
  }

}
