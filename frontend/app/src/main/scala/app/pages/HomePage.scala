package app.pages

import app.services.ApplicationService
import app.support.Navigation
import app.ui.{CompositeSupport, PageComposite}
import jfx.action.Button.{button, buttonType, onClick}
import jfx.core.component.ElementComponent.*
import jfx.dsl.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.Span.span
import jfx.layout.VBox.vbox

class HomePage extends PageComposite("Home") {

  override def pageWidth: Int = 1120
  override def pageHeight: Int = 780

  override protected def compose(using DslContext): Unit = {
    classProperty += "home-page"

    withDslContext {
      val service = inject[ApplicationService]

      val authenticatedProperty =
        service.app.map(app => app != null && app.user != null && app.user.id.get != null)

      vbox {
        classes = "home-page__layout"

        div {
          classes = "home-page__hero"

          div {
            classes = "home-page__hero-orbit home-page__hero-orbit--left"
          }

          div {
            classes = "home-page__hero-orbit home-page__hero-orbit--right"
          }

          vbox {
            classes = "home-page__hero-copy"

            span {
              classes = "home-page__eyebrow"
              text = "Technology Speaks"
            }

            span {
              classes = "home-page__title"
              text = "Wissensraum fuer Spiritualitaet, Philosophie und lebendige Resonanz."
            }

            span {
              classes = "home-page__subtitle"
              text = "Dokumente, Diskurse und Beziehungen liegen hier nicht nebeneinander, sondern greifen ineinander. Schreibe Gedanken aus, verbinde Quellen, teile Impulse und lass daraus Gespraeche entstehen."
            }

            hbox {
              classes = "home-page__actions"

              button("Zum Wissensraum") {
                buttonType = "button"
                classes = "home-page__button home-page__button--primary"
                onClick(_ => Navigation.navigate("/document/documents/document/root"))
              }

              button("Resonanzen ansehen") {
                buttonType = "button"
                classes = "home-page__button home-page__button--secondary"
                onClick(_ => Navigation.navigate("/timeline/posts"))
              }

              button("Anmelden") {
                buttonType = "button"
                classes = "home-page__button home-page__button--ghost"
                style {
                  display <-- authenticatedProperty.map(isAuthenticated => if (isAuthenticated) "none" else "inline-flex")
                }
                onClick(_ => Navigation.navigate("/security/login"))
              }
            }
          }

          div {
            classes = "home-page__hero-panel glass-border"

            vbox {
              classes = "home-page__hero-panel-shell"

              div {
                classes = "home-page__hero-panel-kicker"
                text = "Drei Bewegungen"
              }

              hbox {
                classes = "home-page__signal-row"

                featureCard(
                  icon = "auto_stories",
                  title = "Dokumente",
                  copy = "Lange Gedanken entwickeln, verlinken und als zusammenhaengenden Wissensraum strukturieren."
                )

                featureCard(
                  icon = "forum",
                  title = "Resonanz",
                  copy = "Aus Texten entstehen Aufgaben, Kommentare und Anschlussstellen fuer echte Dialoge."
                )

                featureCard(
                  icon = "hub",
                  title = "Beziehungen",
                  copy = "Menschen, Gruppen und Sichtbarkeit greifen direkt in den Wissensfluss ein."
                )
              }
            }
          }
        }

        hbox {
          classes = "home-page__grid"

          div {
            classes = "home-page__panel glass-border"

            vbox {
              classes = "home-page__panel-shell"

              span {
                classes = "home-page__panel-title"
                text = "Wie man hier arbeitet"
              }

              narrativePoint(
                index = "01",
                title = "Ein Thema als Dokument aufspannen",
                copy = "Nicht nur einen Post schreiben, sondern einen belastbaren Denkraum mit Struktur, Links und Bildern."
              )

              narrativePoint(
                index = "02",
                title = "Resonanz aus dem Text heraus entwickeln",
                copy = "Fragen, Aufgaben und Kommentare sind keine Nebenstrecke, sondern eine Fortsetzung des Gedankens."
              )

              narrativePoint(
                index = "03",
                title = "Sichtbarkeit bewusst steuern",
                copy = "Wer was lesen darf, ist Teil der inhaltlichen Architektur und nicht nur eine technische Einstellung."
              )
            }
          }

          div {
            classes = "home-page__panel glass-border"

            vbox {
              classes = "home-page__panel-shell"

              span {
                classes = "home-page__panel-title"
                text = "Direkte Einstiege"
              }

              quickLink(
                icon = "description",
                title = "Root-Dokument",
                copy = "Direkt in den zentralen Dokumentraum springen.",
                action = () => Navigation.navigate("/document/documents/document/root")
              )

              quickLink(
                icon = "dynamic_feed",
                title = "Timeline",
                copy = "Kurze Gedanken, Reaktionen und Anschlussdiskussionen.",
                action = () => Navigation.navigate("/timeline/posts")
              )

              quickLink(
                icon = "group",
                title = "Menschen",
                copy = "Profile, Beziehungen und Sichtbarkeit von Wissen.",
                action = () => Navigation.navigate("/core/users")
              )
            }
          }
        }
      }
    }
  }

  private def featureCard(icon: String, title: String, copy: String)(using DslContext): Unit = {
    div {
      classes = "home-page__signal-card"

      div {
        classes = "home-page__signal-icon-shell"

        span {
          classes = Seq("material-icons", "home-page__signal-icon")
          text = icon
        }
      }

      vbox {
        classes = "home-page__signal-copy"

        span {
          classes = "home-page__signal-title"
          text = title
        }

        span {
          classes = "home-page__signal-text"
          text = copy
        }
      }
    }
  }

  private def narrativePoint(index: String, title: String, copy: String)(using DslContext): Unit = {
    hbox {
      classes = "home-page__narrative"

      div {
        classes = "home-page__narrative-index"
        text = index
      }

      vbox {
        classes = "home-page__narrative-copy"

        span {
          classes = "home-page__narrative-title"
          text = title
        }

        span {
          classes = "home-page__narrative-text"
          text = copy
        }
      }
    }
  }

  private def quickLink(icon: String, title: String, copy: String, action: () => Unit)(using DslContext): Unit = {
    div {
      classes = "home-page__quick-link"
      summon[jfx.layout.Div].element.onclick = _ => action()

      hbox {
        classes = "home-page__quick-link-row"

        div {
          classes = "home-page__quick-link-icon-shell"

          span {
            classes = Seq("material-icons", "home-page__quick-link-icon")
            text = icon
          }
        }

        vbox {
          classes = "home-page__quick-link-copy"

          span {
            classes = "home-page__quick-link-title"
            text = title
          }

          span {
            classes = "home-page__quick-link-text"
            text = copy
          }
        }

        span {
          classes = Seq("material-icons", "home-page__quick-link-arrow")
          text = "north_east"
        }
      }
    }
  }
}

object HomePage {
  def homePage(init: HomePage ?=> Unit = {})(using Scope): HomePage =
    CompositeSupport.buildPage(new HomePage)(init)
}
