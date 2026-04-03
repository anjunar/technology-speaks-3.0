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
    Api.request("/service/followers/groups/groups").post(this).read[Data[Group]]

  def update(): Future[Data[Group]] =
    Api.request("/service/followers/groups/groups").put(this).read[Data[Group]]

  def delete(): Future[Unit] =
    Api.request("/service/followers/groups/groups").delete(this).unit
}

object Group {
 
  def read(id: String): Future[Data[Group]] =
    Api.request(s"/service/followers/groups/groups/$id").get.read[Data[Group]]

  def list(index: Int, limit: Int, sorting: Seq[String] = Seq("created:desc")): Future[Table[Data[Group]]] =
    Api.request(s"/service/followers/groups?index=$index&limit=$limit${renderSortParameters(sorting)}").get.read[Table[Data[Group]]]

  private def renderSortParameters(sorting: Seq[String]): String = {
    val normalizedSorting = sorting.iterator.map(_.trim).filter(_.nonEmpty).toVector
    if (normalizedSorting.isEmpty) ""
    else normalizedSorting.map(value => s"&sort=${encodeURIComponent(value)}").mkString
  }
}
