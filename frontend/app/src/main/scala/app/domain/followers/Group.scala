package app.domain.followers

import app.domain.core.{AbstractEntity, Data, Link, Table, User}
import app.support.Api
import com.anjunar.scala.enterprise.macros.{PropertyAccess, PropertyMacros}
import jfx.core.state.{ListProperty, Property}
import jfx.form.validators.SizeValidator

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.URIUtils.encodeURIComponent

class Group extends AbstractEntity[Group] {

  val name: Property[String] = Property("")
  val users: ListProperty[User] = ListProperty()

  val editable: Property[Boolean] = Property(false)

  override def properties: Seq[PropertyAccess[Group, ?]] = Group.properties

  def save(): Future[Data[Group]] =
    Api.post("/service/followers/groups/groups", this)

  def update(): Future[Data[Group]] =
    Api.put("/service/followers/groups/groups", this)

  def delete(): Future[Unit] =
    Api.delete("/service/followers/groups/groups", this)
}

object Group {
  val properties: Seq[PropertyAccess[Group, ?]]= PropertyMacros.describeProperties[Group]
  def read(id: String): Future[Data[Group]] =
    Api.get(s"/service/followers/groups/groups/$id")

  def list(index: Int, limit: Int, sorting: Seq[String] = Seq("created:desc")): Future[Table[Data[Group]]] =
    Api.get(s"/service/followers/groups?index=$index&limit=$limit${renderSortParameters(sorting)}")

  private def renderSortParameters(sorting: Seq[String]): String = {
    val normalizedSorting = sorting.iterator.map(_.trim).filter(_.nonEmpty).toVector
    if (normalizedSorting.isEmpty) ""
    else normalizedSorting.map(value => s"&sort=${encodeURIComponent(value)}").mkString
  }
}
