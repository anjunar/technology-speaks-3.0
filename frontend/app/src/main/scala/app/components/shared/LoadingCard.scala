package app.components.shared

import app.ui.{CompositeSupport, DivComposite}
import jfx.core.component.ElementComponent.*
import jfx.dsl.*
import jfx.layout.Div.div
import jfx.layout.VBox.vbox

class LoadingCard extends DivComposite {

  private var minHeightValue: String = "200px"
  private var labelValue: String = "Laden..."

  def minHeight(value: String): Unit =
    minHeightValue = Option(value).filter(_.trim.nonEmpty).getOrElse("200px")

  def label(value: String): Unit =
    labelValue = Option(value).filter(_.trim.nonEmpty).getOrElse("Laden...")

  override protected def compose(using DslContext): Unit = {
    classProperty += "glass-border"

    withDslContext {
      vbox {
        style {
          setProperty("min-height", minHeightValue)
          justifyContent = "center"
          alignItems = "center"
        }

        div {
          text = labelValue
        }
      }
    }
  }
}

object LoadingCard {
  def loadingCard(init: LoadingCard ?=> Unit = {}): LoadingCard =
    CompositeSupport.buildComposite(new LoadingCard)(init)
}
