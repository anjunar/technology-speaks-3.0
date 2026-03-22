package com.anjunar.technologyspeaks.shared.editor

import com.anjunar.json.mapper.ObjectMapperProvider
import org.hibernate.`type`.descriptor.WrapperOptions
import org.hibernate.usertype.UserType

import java.io.Serializable
import java.sql.{PreparedStatement, ResultSet, Types}
import java.util.Objects

class NodeType extends UserType[Node] {

  private val objectMapper = ObjectMapperProvider.mapper

  override def getSqlType(): Int = Types.OTHER

  override def returnedClass(): Class[Node] = classOf[Node]

  override def equals(x: Node, y: Node): Boolean = Objects.equals(x, y)

  override def hashCode(x: Node): Int = Objects.hashCode(x)

  override def nullSafeGet(
    rs: ResultSet,
    position: Int,
    options: WrapperOptions
  ): Node = {
    val json = rs.getString(position)
    if (json == null) null else objectMapper.readValue(json, classOf[Node])
  }

  override def nullSafeSet(
    st: PreparedStatement,
    value: Node,
    position: Int,
    options: WrapperOptions
  ): Unit = {
    if (value == null) {
      st.setNull(position, Types.OTHER)
    } else {
      val json = objectMapper.writeValueAsString(value)
      st.setObject(position, json, Types.OTHER)
    }
  }

  override def deepCopy(value: Node): Node = {
    if (value == null) {
      null
    } else {
      val json = objectMapper.writeValueAsString(value)
      objectMapper.readValue(json, classOf[Node])
    }
  }

  override def isMutable(): Boolean = true

  override def disassemble(value: Node): Serializable =
    deepCopy(value).asInstanceOf[Serializable]

  override def assemble(cached: Serializable, owner: Any): Node =
    deepCopy(cached.asInstanceOf[Node])

}
