package app.domain.followers

import app.domain.core.{AbstractEntity, Data, Link, Table, User}
import app.support.Api
import app.support.Api.given
import jfx.core.meta.Meta
import jfx.core.state.{ListProperty, Property}
import jfx.form.validators.*

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.URIUtils.encodeURIComponent

class Group extends AbstractEntity {

  @NotBlank(message = "Name ist erforderlich")
  @Size(min = 3, max = 80, message = "Name muss zwischen 3 und 80 Zeichen haben")
  val name: Property[String] = Property("")
  val users: ListProperty[User] = ListProperty()

  val editable: Property[Boolean] = Property(false)

  def save(): Future[Data[Group]] =
    Api.post("/service/followers/groups/groups", this).map(raw => Api.deserialize[Data[Group]](raw))

  def update(): Future[Data[Group]] =
    Api.put("/service/followers/groups/groups", this).map(raw => Api.deserialize[Data[Group]](raw))

  def delete(): Future[Unit] =
    Api.delete("/service/followers/groups/groups", this)
}

object Group {
 
  def read(id: String): Future[Data[Group]] =
    Api.get(s"/service/followers/groups/groups/$id").map(raw => Api.deserialize[Data[Group]](raw))

  def list(index: Int, limit: Int, sorting: Seq[String] = Seq("created:desc")): Future[Table[Data[Group]]] =
    Api.get(s"/service/followers/groups?index=$index&limit=$limit${renderSortParameters(sorting)}").map(raw => Api.deserialize[Table[Data[Group]]](raw))

  private def renderSortParameters(sorting: Seq[String]): String = {
    val normalizedSorting = sorting.iterator.map(_.trim).filter(_.nonEmpty).toVector
    if (normalizedSorting.isEmpty) ""
    else normalizedSorting.map(value => s"&sort=${encodeURIComponent(value)}").mkString
  }
}
