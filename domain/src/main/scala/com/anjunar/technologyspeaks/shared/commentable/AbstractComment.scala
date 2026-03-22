package com.anjunar.technologyspeaks.shared.commentable

import com.anjunar.json.mapper.provider.{EntityProvider, OwnerProvider}
import com.anjunar.technologyspeaks.core.{AbstractEntity, User}
import com.anjunar.technologyspeaks.shared.editor.{Node, NodeType}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.{Column, ManyToOne, MappedSuperclass}
import org.hibernate.annotations.Type

import scala.beans.BeanProperty

@MappedSuperclass
abstract class AbstractComment extends AbstractEntity with OwnerProvider {

  @ManyToOne(optional = false)
  @JsonbProperty
  @BeanProperty
  var user: User = null

  @Column(columnDefinition = "jsonb")
  @Type(value = classOf[NodeType])
  @JsonbProperty
  @BeanProperty
  var editor: Node = null

  override def owner(): EntityProvider = user

}
