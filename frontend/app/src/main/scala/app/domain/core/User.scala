package app.domain.core

import app.support.{Api, Navigation}
import jfx.core.macros.{property, typedProperty}
import jfx.core.state.{ListProperty, Property, PropertyAccess}
import jfx.domain.Media

import scala.concurrent.ExecutionContext.Implicits.global
import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.URIUtils.encodeURIComponent

class User extends AbstractEntity[User] {

  val nickName: Property[String] = Property("")
  val image: Property[Media | Null] = Property(null)
  val info: Property[UserInfo | Null] = Property(null)
  val address: Property[Address | Null] = Property(null)
  val emails: ListProperty[Email] = ListProperty()

  override def properties: js.Array[PropertyAccess[User, ?]] =
    User.properties

  def save(): Future[Data[User]] =
    Api.post("/service/core/users/user", this)

  def update(): Future[Data[User]] =
    Api.put("/service/core/users/user", this)

  def delete(): Future[Unit] =
    Api.delete("/service/core/users/user", this)

  def followLink: Option[Link] =
    links.find(_.rel == "follow")

  def unfollowLink: Option[Link] =
    links.find(_.rel == "unfollow")

  def followActionLink: Option[Link] =
    unfollowLink.orElse(followLink)

  def isFollowed: Boolean =
    unfollowLink.isDefined

  def invokeFollowAction(): Future[User] =
    followActionLink match {
      case Some(link) if id.get != null =>
        Api
          .requestJson(link.method, Navigation.prefixedServiceUrl(link.url))
          .flatMap(_ => User.read(id.get.toString))
          .map { response =>
            links.setAll(response.data.links)
            this
          }
      case _ =>
        Future.successful(this)
    }
}

object User {
  val properties: js.Array[PropertyAccess[User, ?]] = js.Array(
    typedProperty[User, Property[UUID], UUID](_.id),
    typedProperty[User, Property[String | Null], String | Null](_.modified),
    typedProperty[User, Property[String | Null], String | Null](_.created),
    typedProperty[User, Property[String], String](_.nickName),
    typedProperty[User, Property[Media | Null], Media | Null](_.image),
    typedProperty[User, Property[UserInfo | Null], UserInfo | Null](_.info),
    typedProperty[User, Property[Address | Null], Address | Null](_.address),
    typedProperty[User, ListProperty[Email], Email](_.emails),
    typedProperty[User, ListProperty[Link], Link](_.links)
  )

  def read(id: String): Future[Data[User]] =
    Api.get(s"/service/core/users/user/$id")

  def list(index: Int, limit: Int, query: String = "", sorting: Seq[String] = Seq("created:desc")): Future[Table[Data[User]]] = {
    val normalizedQuery = Option(query).map(_.trim).getOrElse("")
    val queryParameter =
      if (normalizedQuery.isEmpty) ""
      else s"&name=${encodeURIComponent(normalizedQuery)}"
    val sortParameter = renderSortParameters(sorting)

    Api.get(s"/service/core/users?index=$index&limit=$limit$sortParameter$queryParameter")
  }

  private def renderSortParameters(sorting: Seq[String]): String = {
    val normalizedSorting = sorting.iterator.map(_.trim).filter(_.nonEmpty).toVector
    if (normalizedSorting.isEmpty) ""
    else normalizedSorting.map(value => s"&sort=${encodeURIComponent(value)}").mkString
  }
}
