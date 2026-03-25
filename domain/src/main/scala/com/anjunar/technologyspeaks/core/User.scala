package com.anjunar.technologyspeaks.core

import com.anjunar.json.mapper.provider.{EntityProvider, OwnerProvider}
import com.anjunar.json.mapper.schema.{EntitySchema, SchemaProvider}
import com.anjunar.technologyspeaks.SpringContext
import com.anjunar.technologyspeaks.hibernate.{EntityContext, RepositoryContext}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.*
import jakarta.validation.constraints.{NotBlank, Size}

import scala.annotation.meta.field
import scala.beans.BeanProperty
import java.util
import scala.compiletime.uninitialized

@Entity
@Table(name = "Core#User")
@NamedEntityGraphs(
  value = Array(
    new NamedEntityGraph(
      name = "User.full",
      attributeNodes = Array(
        new NamedAttributeNode("id"),
        new NamedAttributeNode("nickName"),
        new NamedAttributeNode("image"),
        new NamedAttributeNode("info"),
        new NamedAttributeNode("address"),
        new NamedAttributeNode("emails")
      )
    )
  )
)
class User(@(JsonbProperty @field) @(NotBlank @field) @(Size @field)(min = 2, max = 80) var nickName: String)
  extends AbstractEntity, EntityContext[User], OwnerProvider {

  def this() = this(null)

  @ManyToOne(cascade = Array(CascadeType.ALL), fetch = FetchType.LAZY)
  @JsonbProperty
  var image: Media = uninitialized

  @OneToOne(cascade = Array(CascadeType.ALL))
  @JsonbProperty
  var info: UserInfo = uninitialized

  @OneToOne(cascade = Array(CascadeType.ALL))
  @JsonbProperty
  var address: Address = uninitialized

  @OneToMany(cascade = Array(CascadeType.ALL), orphanRemoval = true, mappedBy = "user")
  @JsonbProperty
  val emails: util.Set[EMail] = new util.HashSet[EMail]()

  override def owner(): EntityProvider = this

}

object User extends RepositoryContext[User] with SchemaProvider {

  override def schema(): EntitySchema[?] = new Schema

  open class Schema extends AbstractEntitySchema[User] {
    @JsonbProperty val nickName = property(_.nickName, new OwnerRule[User]())
    @JsonbProperty val image = property(_.image, new OwnerRule[User]())
    @JsonbProperty val info = property(_.info, new OwnerRule[User]())
    @JsonbProperty val address = property(_.address, new OwnerRule[User]())
    @JsonbProperty val emails = property(_.emails, new ManagedRule[User]())
  }

  @Entity(name = "UserView")
  class View extends EntityView

  def findViewByUser(user: User): EntityView = {
    val entityManager = SpringContext.entityManager()

    try {
      entityManager
        .createQuery("select v from UserView v left join fetch v.properties where v.user = :user", classOf[View])
        .setParameter("user", user)
        .getSingleResult
    } catch {
      case _: NoResultException => null
    }
  }

}
