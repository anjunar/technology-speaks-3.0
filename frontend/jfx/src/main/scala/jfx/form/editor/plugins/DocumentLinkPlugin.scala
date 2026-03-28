package jfx.form.editor.plugins

import jfx.action.Button
import jfx.core.component.CompositeComponent
import jfx.core.component.ElementComponent.*
import jfx.core.state.Property.subscribeBidirectional
import jfx.core.state.{ListProperty, Property}
import jfx.dsl.*
import jfx.form.Input
import jfx.layout.{Div, HBox, VBox, Viewport}
import jfx.statement.ForEach.forEach
import org.scalajs.dom.HTMLDivElement

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.timers.{SetTimeoutHandle, clearTimeout, setTimeout}
import scala.util.{Failure, Success}

final case class DocumentLinkSuggestion(
  href: String,
  title: String,
  subtitle: String | Null = null
)

class DocumentLinkPlugin extends LinkPlugin {

  private given ExecutionContext = ExecutionContext.global

  var searchLimit: Int = 8
  var searchDocuments: (String, Int) => Future[Seq[DocumentLinkSuggestion]] =
    (_, _) => Future.successful(Seq.empty)

  override protected def openLinkEditor(attrs: js.Dynamic): Unit = {
    given Scope = currentPluginScope

    val initialHref = attrString(attrs, "href").orNull
    val initialTitle = attrString(attrs, "title").orNull

    Viewport.addWindow(
      new Viewport.WindowConf(
        title = "Dokument verlinken",
        width = 640,
        height = 620,
        component = Viewport.captureComponent {
          CompositeComponent.composite(
            new DocumentLinkDialog(
              initialHref = initialHref,
              initialTitle = initialTitle,
              searchLimit = searchLimit,
              searchDocuments = searchDocuments,
              applyLink = (href, title) => this.insertLink(href, title),
              applyLinkText = (href, title, label) => this.insertLinkText(href, title, label),
              removeLink = () => this.removeLink()
            )
          )
        }
      )
    )
  }
}

object DocumentLinkPlugin {

  def documentLinkPlugin(init: DocumentLinkPlugin ?=> Unit = {}): DocumentLinkPlugin =
    DslRuntime.currentScope { currentScope =>
      val currentContext = DslRuntime.currentComponentContext()
      val component = new DocumentLinkPlugin()
      component.captureScope(currentScope)

      DslRuntime.withComponentContext(ComponentContext(Some(component), currentContext.enclosingForm)) {
        given Scope = currentScope
        given DocumentLinkPlugin = component
        init
      }

      DslRuntime.attach(component, currentContext)
      component
    }
}

private final class DocumentLinkDialog(
  initialHref: String | Null,
  initialTitle: String | Null,
  searchLimit: Int,
  searchDocuments: (String, Int) => Future[Seq[DocumentLinkSuggestion]],
  applyLink: (String, String | Null) => Unit,
  applyLinkText: (String, String | Null, String) => Unit,
  removeLink: () => Unit
) extends CompositeComponent[HTMLDivElement]
    with Viewport.CloseAware {

  private given ExecutionContext = ExecutionContext.global
  protected type DslContext = CompositeComponent.DslContext

  override val element: HTMLDivElement = newElement("div")

  private val hrefProperty = Property(Option(initialHref).getOrElse(""))
  private val titleProperty = Property(Option(initialTitle).getOrElse(""))
  private val searchQueryProperty = Property("")
  private val loadingProperty = Property(false)
  private val errorProperty = Property("")
  private val resultsProperty = ListProperty[DocumentLinkSuggestion]()

  private var closeWindow: () => Unit = () => ()
  private var pendingReload: SetTimeoutHandle | Null = null
  private var requestVersion = 0

  addDisposable(searchQueryProperty.observeWithoutInitial(_ => scheduleReload()))
  addDisposable(() => cancelScheduledReload())

  override def close_=(callback: () => Unit): Unit =
    closeWindow = callback

  override protected def compose(using DslContext): Unit = {
    classProperty += "document-link-dialog"

    withDslContext {
      VBox.vbox {
        classes = "document-link-dialog__shell"
        style {
          rowGap = "12px"
          width = "100%"
          height = "100%"
        }

        Div.div {
          classes = "document-link-dialog__intro"
          text = "Waehle ein Dokument aus der Liste oder hinterlege den Link manuell."
        }

        Div.div {
          classes = "document-link-dialog__section-title"
          text = "Dokument suchen"
        }

        Div.div {
          classes = "document-link-dialog__search"

          Div.div {
            classes = Seq("material-icons", "document-link-dialog__search-icon")
            text = "search"
          }

          Input.input("document-search") {
            val current = summon[Input]
            current.classProperty += "document-link-dialog__search-input"
            current.placeholder = "Titel eingeben..."
            subscribeBidirectional(searchQueryProperty, current.stringValueProperty)
          }
        }

        Div.div {
          classes = "document-link-dialog__status"
          style {
            display <-- loadingProperty.map(isLoading => if (isLoading) "block" else "none")
          }
          text = "Suche..."
        }

        Div.div {
          classes = Seq("document-link-dialog__status", "document-link-dialog__status--error")
          style {
            display <-- errorProperty.map(message => if (message.trim.nonEmpty) "block" else "none")
          }
          subscribeBidirectional(errorProperty, textProperty)
        }

        Div.div {
          classes = "document-link-dialog__results"
          style {
            flex = "1"
            minHeight = "0px"
          }

          Div.div {
            classes = "document-link-dialog__status"
            style {
              display <-- resultsProperty.map(items =>
                if (items.isEmpty && !loadingProperty.get && errorProperty.get.trim.isEmpty) "block" else "none"
              )
            }
            text = "Keine Dokumente gefunden."
          }

          forEach(resultsProperty) { suggestion =>
            Div.div {
              classes = "document-link-dialog__result"
              style {
                cursor = "pointer"
              }

              summon[Div].element.onclick = _ => insertSelectedSuggestion(suggestion)

              HBox.hbox {
                classes = "document-link-dialog__result-row"
                style {
                  alignItems = "center"
                  columnGap = "12px"
                }

                VBox.vbox {
                  classes = "document-link-dialog__result-copy"
                  style {
                    flex = "1"
                    minWidth = "0px"
                    rowGap = "4px"
                  }

                  Div.div {
                    classes = "document-link-dialog__result-title"
                    text = suggestion.title
                  }

                  Div.div {
                    classes = "document-link-dialog__result-subtitle"
                    text = Option(suggestion.subtitle).getOrElse(suggestion.href)
                  }
                }

                Button.button("Einfuegen") {
                  val current = summon[Button]
                  current.buttonType = "button"
                  current.classProperty.setAll(Seq("doc-new-btn", "document-link-dialog__pick"))
                  current.addClick { event =>
                    event.stopPropagation()
                    insertSelectedSuggestion(suggestion)
                  }
                }
              }
            }
          }
        }

        Div.div {
          classes = "document-link-dialog__manual"

          VBox.vbox {
            style {
              rowGap = "10px"
            }

            Div.div {
              classes = "document-link-dialog__section-title"
              text = "Link manuell"
            }

            Input.input("href") {
              val current = summon[Input]
              current.classProperty += "document-link-dialog__input"
              current.placeholder = "https://example.com oder /document/documents/document/..."
              subscribeBidirectional(hrefProperty, current.stringValueProperty)
            }

            Input.input("title") {
              val current = summon[Input]
              current.classProperty += "document-link-dialog__input"
              current.placeholder = "Titel (optional)"
              subscribeBidirectional(titleProperty, current.stringValueProperty)
            }
          }
        }

        HBox.hbox {
          classes = "document-link-dialog__actions"
          style {
            columnGap = "10px"
            justifyContent = "flex-end"
          }

          Button.button("Speichern") {
            val current = summon[Button]
            current.buttonType = "button"
            current.classProperty += "doc-new-btn"
            current.addClick { _ =>
              saveCurrentLink()
            }
          }

          Button.button("Link entfernen") {
            val current = summon[Button]
            current.buttonType = "button"
            current.classProperty += "document-link-dialog__remove"
            current.addClick { _ =>
              removeLink()
              closeWindow()
            }
          }
        }
      }
    }

    reloadResults()
  }

  private def scheduleReload(): Unit = {
    cancelScheduledReload()
    pendingReload = setTimeout(250) {
      reloadResults()
    }
  }

  private def cancelScheduledReload(): Unit =
    if (pendingReload != null) {
      clearTimeout(pendingReload.nn)
      pendingReload = null
    }

  private def reloadResults(): Unit = {
    val currentQuery = Option(searchQueryProperty.get).map(_.trim).getOrElse("")
    requestVersion += 1
    val currentRequest = requestVersion

    loadingProperty.set(true)
    errorProperty.set("")

    searchDocuments(currentQuery, searchLimit).onComplete {
      case Success(results) if currentRequest == requestVersion =>
        loadingProperty.set(false)
        resultsProperty.setAll(results)
      case Failure(error) if currentRequest == requestVersion =>
        loadingProperty.set(false)
        resultsProperty.clear()
        errorProperty.set(Option(error.getMessage).getOrElse("Dokumente konnten nicht geladen werden."))
      case _ =>
        ()
    }
  }

  private def insertSelectedSuggestion(suggestion: DocumentLinkSuggestion): Unit = {
    hrefProperty.set(suggestion.href)
    if (titleProperty.get.trim.isEmpty) {
      titleProperty.set(suggestion.title)
    }

    val href = Option(suggestion.href).map(_.trim).getOrElse("")
    val title =
      Option(titleProperty.get)
        .map(_.trim)
        .filter(_.nonEmpty)
        .orElse(Option(suggestion.title).map(_.trim).filter(_.nonEmpty))
        .orNull

    if (href.isBlank) {
      removeLink()
    } else {
      applyLinkText(href, title, suggestion.title)
    }

    closeWindow()
  }

  private def saveCurrentLink(): Unit = {
    val href = Option(hrefProperty.get).map(_.trim).getOrElse("")
    val title = Option(titleProperty.get).map(_.trim).getOrElse("")

    if (href.isBlank) {
      removeLink()
    } else {
      applyLink(href, if (title.isBlank) null else title)
    }

    closeWindow()
  }
}
