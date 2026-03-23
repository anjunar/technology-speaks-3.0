package app.pages.followers

import app.components.shared.LoadingCard.loadingCard
import app.domain.core.{Data, MediaHelper}
import app.domain.followers.RelationShip
import app.support.{Navigation, RemotePageQuery, RemoteTableList}
import app.ui.{CompositeSupport, DivComposite, PageComposite}
import jfx.control.Image.image
import jfx.control.virtualList
import jfx.core.component.ElementComponent.*
import jfx.core.state.RemoteListProperty
import jfx.dsl.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox

import scala.concurrent.ExecutionContext

class RelationShipsPage extends PageComposite("Following") {

  private given ExecutionContext = ExecutionContext.global
  private val pageSize = 50
  private val relationShipsProperty: RemoteListProperty[Data[RelationShip], RemotePageQuery] =
    RemoteTableList.create[Data[RelationShip]](pageSize = pageSize) { (index, limit) =>
      RelationShip.list(index, limit)
    }

  override protected def compose(using DslContext): Unit = {
    classProperty += "relation-ships-page"

    withDslContext {
      vbox {
        style {
          setProperty("height", "100%")
          padding = "12px"
          boxSizing = "border-box"
        }

        div {
          style {
            flex = "1"
            minHeight = "0px"
          }

          virtualList(relationShipsProperty, estimateHeightPx = 124, overscanPx = 240, prefetchItems = 50) { (relationShip, _) =>
            if (relationShip == null) {
              val card = loadingCard {}
              card.minHeight("112px")
              card
            } else {
              RelationShipListItem.item(relationShip)
            }
          }
        }
      }
    }
  }
}

object RelationShipsPage {
  def relationShipsPage(init: RelationShipsPage ?=> Unit = {}): RelationShipsPage =
    CompositeSupport.buildPage(new RelationShipsPage)(init)
}

private final class RelationShipListItem(data: Data[RelationShip]) extends DivComposite {

  override protected def compose(using DslContext): Unit = {
    classProperty += "glass-border"

    withDslContext {
      hbox {
        style {
          alignItems = "center"
          columnGap = "12px"
          cursor = "pointer"
        }

        Option(data.data.follower.get).flatMap(user => Option(user.image.get)) match {
          case Some(media) =>
            val imageView = image {
              style {
                width = "64px"
                height = "64px"
                borderRadius = "50%"
              }
            }
            imageView.src = MediaHelper.thumbnailLink(media)
          case None =>
            div {
              classes = "material-icons"
              style {
                fontSize = "64px"
              }
              text = "account_circle"
            }
        }

        vbox {
          val follower = Option(data.data.follower.get)

          div {
            style {
              fontWeight = "600"
            }
            text = follower.map(_.nickName.get).getOrElse("User")
          }

          div {
            text =
              follower
                .flatMap(user => Option(user.info.get))
                .map(info => s"${info.firstName.get} ${info.lastName.get}".trim)
                .filter(_.nonEmpty)
                .getOrElse("")
          }

          div {
            text =
              if (data.data.groups.isEmpty) "Keine Gruppe"
              else data.data.groups.map(_.name.get).mkString(", ")
          }
        }
      }

      Option(data.data.follower.get).foreach { follower =>
        element.onclick = _ => Navigation.navigate(s"/core/users/user/${follower.id.get}")
      }
    }
  }
}

private object RelationShipListItem {
  def item(data: Data[RelationShip]): RelationShipListItem =
    CompositeSupport.buildComposite(new RelationShipListItem(data))
}
