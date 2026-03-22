package com.anjunar.technologyspeaks.shared.likeable

import com.anjunar.technologyspeaks.security.IdentityHolder
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service

@Service
class LikeService(
  private val identityHolder: IdentityHolder,
  private val entityManager: EntityManager
) {

  def toggle(entity: LikeContainer.Interface): java.util.Set[Like] = {
    val userId = identityHolder.user.id
    val existing = entity.likes.stream().filter(like => like.user.id == userId).toList()

    if (!existing.isEmpty) {
      existing.forEach(like => {
        entity.likes.remove(like)
        entityManager.remove(like)
      })
      return entity.likes
    }

    val like = new Like()
    like.user = identityHolder.user
    entityManager.persist(like)
    entity.likes.add(like)

    entity.likes
  }

}
