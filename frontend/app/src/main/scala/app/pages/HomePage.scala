package app.pages

import app.domain.core.{Data, User}
import app.support.{Navigation, RemotePageQuery, RemoteTableList}
import app.ui.{CompositeSupport, PageComposite}
import jfx.core.component.ElementComponent.*
import jfx.core.state.RemoteListProperty
import jfx.dsl.*
import jfx.form.ComboBox.{comboBox, itemRenderer_=, items_=, valueRenderer_=}
import jfx.layout.Div.div
import jfx.layout.VBox.vbox

import scala.concurrent.ExecutionContext

class HomePage extends PageComposite("Home") {

  private given ExecutionContext = ExecutionContext.global
  private val pageSize = 100
  private val usersProperty: RemoteListProperty[Data[User], RemotePageQuery] =
    RemoteTableList.create[Data[User]](pageSize = pageSize) { (index, limit) =>
      User.list(index, limit)
    }

  override protected def compose(using DslContext): Unit = {
    classProperty += "home-page"
    RemoteTableList.reloadFirstPage(usersProperty, pageSize = pageSize)

    withDslContext {
      vbox {
        style {
          rowGap = "12px"
          padding = "12px"
        }

        div {
          text = "Home"
        }

        val selector = comboBox[Data[User]]("users") {
          items_=(usersProperty)
          summon[jfx.form.ComboBox[Data[User]]].placeholder = "Benutzer waehlen"
          itemRenderer_={
            div {
              text = summon[jfx.form.ComboBox.ItemRenderContext[Data[User]]].item.data.nickName.get
            }
          }
          valueRenderer_={
            div {
              text =
                Option(summon[jfx.form.ComboBox.ValueRenderContext[Data[User]]].selectedItem)
                  .map(_.data.nickName.get)
                  .getOrElse("Benutzer waehlen")
            }
          }
        }

        addDisposable(
          selector.selectedItemProperty.observe { selected =>
            if (selected != null && selected.data.id.get != null) {
              Navigation.navigate(s"/core/users/user/${selected.data.id.get}")
            }
          }
        )
      }
    }
  }
}

object HomePage {
  def homePage(init: HomePage ?=> Unit = {}): HomePage =
    CompositeSupport.buildPage(new HomePage)(init)
}
