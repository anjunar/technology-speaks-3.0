package com.anjunar.technologyspeaks.shared.likeable

object LikeContainer {

  trait Interface {
    def likes: java.util.Set[Like]
  }

}
