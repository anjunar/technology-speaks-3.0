package app.domain.core

import app.support.{Api, Navigation}
import jfx.core.meta.Meta
import jfx.core.state.{ListProperty, Property}
import jfx.domain.Media

import scala.concurrent.ExecutionContext.Implicits.global
import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.js
import jfx.form.validators.*
import scala.scalajs.js.URIUtils.encodeURIComponent

class User extends AbstractEntity {

  @NotBlank(message = "NickName ist erforderlich")
  @Size(min = 2, max = 80, message = "NickName muss zwischen 2 und 80 Zeichen haben")
  val nickName: Property[String] = Property("")
  val image: Property[Media | Null] = Property(null)
  val info: Property[UserInfo | Null] = Property(null)
  val address: Property[Address | Null] = Property(null)
  val emails: ListProperty[EMail] = ListProperty()

  def save(): Future[Data[User]] =
    Api.request("/service/core/users/user").post(this).read[Data[User]]

  def update(): Future[Data[User]] =
    Api.request("/service/core/users/user").put(this).read[Data[User]]

  def delete(): Future[Unit] =
    Api.request("/service/core/users/user").delete(this).unit

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
          .link(link)
          .invoke
          .unit
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

  def read(id: String): Future[Data[User]] =
    Api.request(s"/service/core/users/user/$id").get.read[Data[User]]

  def list(index: Int, limit: Int, query: String = "", sorting: Seq[String] = Seq("created:desc")): Future[Table[Data[User]]] = {
    val normalizedQuery = Option(query).map(_.trim).getOrElse("")
    val queryParameter =
      if (normalizedQuery.isEmpty) ""
      else s"&name=${encodeURIComponent(normalizedQuery)}"
    val sortParameter = renderSortParameters(sorting)

    Api.request(s"/service/core/users?index=$index&limit=$limit$sortParameter$queryParameter").get.read[Table[Data[User]]]
  }

  private def renderSortParameters(sorting: Seq[String]): String = {
    val normalizedSorting = sorting.iterator.map(_.trim).filter(_.nonEmpty).toVector
    if (normalizedSorting.isEmpty) ""
    else normalizedSorting.map(value => s"&sort=${encodeURIComponent(value)}").mkString
  }
}
