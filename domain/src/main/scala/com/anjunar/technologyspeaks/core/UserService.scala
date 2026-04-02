package com.anjunar.technologyspeaks.core

import com.anjunar.technologyspeaks.documents.{Document, Issue}
import com.anjunar.technologyspeaks.followers.{Group, RelationShip}
import com.anjunar.technologyspeaks.rest.EntityManagerProvider
import com.anjunar.technologyspeaks.shared.commentable.{FirstComment, SecondComment}
import com.anjunar.technologyspeaks.timeline.Post
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import scala.jdk.CollectionConverters.*

@Service
class UserService(val entityManager: EntityManager) extends EntityManagerProvider {

  @Transactional(readOnly = false)
  def deleteUser(user: User): Unit = {
    Post.queryAll("user" -> user).asScala.foreach(_.remove())

    RelationShip.queryAll("user" -> user).asScala.foreach(_.remove())
    RelationShip.queryAll("follower" -> user).asScala.foreach(_.remove())

    Group.queryAll("user" -> user).asScala.foreach { group =>
      deleteGroup(group)
    }

    entityManager.createQuery("delete from Like l where l.user = :user")
      .setParameter("user", user)
      .executeUpdate()

    FirstComment.queryAll("user" -> user).asScala.foreach(_.remove())
    SecondComment.queryAll("user" -> user).asScala.foreach(_.remove())

    Document.queryAll("user" -> user).asScala.foreach(_.remove())
    Issue.queryAll("user" -> user).asScala.foreach(_.remove())

    entityManager.createQuery("select mp from ManagedProperty mp left join fetch mp.view join mp.users u where u = :user", classOf[ManagedProperty])
      .setParameter("user", user)
      .getResultList
      .asScala
      .foreach { mp =>
        mp.users.remove(user)
      }

    val userView = User.findViewByUser(user)
    if (userView != null) {
      userView.properties.asScala.toList.foreach { managedProperty =>
        managedProperty.users.clear()
        managedProperty.groups.clear()
        userView.properties.remove(managedProperty)
      }
      entityManager.remove(userView)
    }

    user.remove()
  }

  @Transactional(readOnly = false)
  def deleteGroup(group: Group): Unit = {
    entityManager.createQuery("select r from RelationShip r join r.groups g where g = :group", classOf[RelationShip])
      .setParameter("group", group)
      .getResultList
      .asScala
      .foreach { r =>
        r.groups.remove(group)
      }

    entityManager.createQuery("select mp from ManagedProperty mp left join fetch mp.view join mp.groups g where g = :group", classOf[ManagedProperty])
      .setParameter("group", group)
      .getResultList
      .asScala
      .foreach { mp =>
        mp.groups.remove(group)
      }

    group.remove()
  }
}
