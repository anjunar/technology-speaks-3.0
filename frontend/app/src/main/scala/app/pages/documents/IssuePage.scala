package app.pages.documents

import app.components.commentable.FirstCommentCard.firstCommentCard
import app.components.shared.LoadingCard.loadingCard
import app.domain.core.{AbstractEntity, Data, Table}
import app.domain.documents.{Issue, IssueCreated, IssueUpdated}
import app.domain.shared.FirstComment
import app.services.ApplicationService
import app.support.Navigation.renderByRel
import app.support.{Api, RemotePageQuery, RemoteTableList}
import app.ui.{CompositeSupport, DivComposite, PageComposite}
import jfx.action.Button.{button, buttonType, onClick}
import jfx.control.virtualList
import jfx.core.component.ElementComponent.*
import jfx.core.state.Property.subscribeBidirectional
import jfx.core.state.{ListProperty, RemoteListProperty}
import jfx.dsl.*
import jfx.form.Control.valueProperty
import jfx.form.Editable.editableProperty
import jfx.form.Editor.editor
import jfx.form.{ErrorResponseException, Form}
import jfx.form.Form.{form, onSubmit}
import jfx.form.Input.{disabled, input, standaloneInput, stringValueProperty}
import jfx.form.InputContainer.inputContainer
import jfx.form.editor.plugins.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.Span.span
import jfx.layout.VBox.vbox
import jfx.layout.Viewport

import scala.concurrent.{ExecutionContext, Future}

class IssuePage(val model: Issue) extends PageComposite("Aufgabe", pageResizable = true) {

  override def pageWidth: Int = 980
  override def pageHeight: Int = 860

  private given ExecutionContext = ExecutionContext.global
  private val pageSize = 50
  private val itemsProperty: ListProperty[Data[? <: AbstractEntity[?]]] = ListProperty()

  private val commentsProperty: RemoteListProperty[FirstComment, RemotePageQuery] =
    RemoteTableList.createMapped[Data[FirstComment], FirstComment](pageSize = pageSize) { query =>
      if (canLoadComments) {
        FirstComment.list(query.index, query.limit, model)
      } else {
        Future.successful(new Table[Data[FirstComment]]())
      }
    }(_.data)

  private def canLoadComments: Boolean =
    model.links.exists(link => Option(link.url).exists(_.contains("/comments")))

  private def canCreateComments: Boolean =
    model.links.exists(link => Option(link.url).exists(_.endsWith("/comment")))

  private def persistIssue(service: ApplicationService): Unit = {
    try {
      model.links.find(link => link.rel == "update" || link.rel == "save").foreach { link =>
        Api.invokeLink(link, model).map(raw => Api.deserialize(raw, Data.meta[Issue])).foreach { saved =>
          if (link.rel == "update") {
            service.messageBus.publish(new IssueUpdated(saved))
          } else {
            service.messageBus.publish(new IssueCreated(saved))
          }

          Viewport.notify("Aufgabe gespeichert!", Viewport.NotificationKind.Success)
          close()
        }
      }
    } catch {
      case _: ErrorResponseException =>
        Viewport.notify("Fehler beim Speichern", Viewport.NotificationKind.Error)
    }
  }

  private def upsertComment(current: FirstComment, saved: FirstComment): Unit = {
    val index = commentsProperty.indexWhere(_ eq current)

    if (index >= 0) commentsProperty.update(index, saved)
    else commentsProperty += saved
    syncItems()
  }

  private def removeComment(comment: FirstComment): Unit =
    commentsProperty.indexWhere(_ eq comment) match {
      case index if index >= 0 =>
        commentsProperty.remove(index)
        syncItems()
      case _ =>
        ()
    }

  private def appendNewComment(service: ApplicationService): Unit = {
    if (!commentsProperty.lastOption.exists(_.editable.get)) {
      val comment = new FirstComment()
      comment.editable.set(true)
      comment.user.set(service.app.get.user)
      commentsProperty += comment
      syncItems()
    }
  }

  override protected def compose(using DslContext): Unit = {
    classProperty.setAll(Seq("issue-page"))

    syncItems()
    addDisposable(commentsProperty.observe(_ => syncItems()))

    if (canLoadComments) {
      RemoteTableList.reloadFirstPage(commentsProperty, pageSize = pageSize)
    } else {
      commentsProperty.clear()
    }

    withDslContext {
      val service = inject[ApplicationService]

      vbox {
        classes = "issue-page__layout"

        vbox {
          classes = "issue-page__hero"

          vbox {
            classes = "issue-page__hero-copy"

            span {
              classes = "issue-page__eyebrow"
              text = "Resonanz"
            }

            span {
              classes = "issue-page__title"
              text = "Aufgabe und Diskussion"
            }
          }
        }

        div {
          classes = "issue-page__feed"
          style {
            flex = "1"
            minHeight = "0px"
          }

          virtualList(itemsProperty, estimateHeightPx = 240, overscanPx = 240, prefetchItems = 40) { (item, _) =>
            if (item == null) {
              loadingCard {
                summon[app.components.shared.LoadingCard].cardMinHeight("160px")
              }
            } else {
              item.data match {
                case issue: Issue =>
                  IssuePage.IssueViewIssueCard.card(issue, () => persistIssue(service))
                case comment: FirstComment =>
                  firstCommentCard(
                    comment = comment,
                    owner = model,
                    onPersist = saved => upsertComment(comment, saved),
                    onDeleteCompleted = saved => removeComment(saved)
                  )
                case _ =>
                  loadingCard {
                    summon[app.components.shared.LoadingCard].cardMinHeight("160px")
                  }
              }
            }
          }
        }

        if (canCreateComments) {
          val commentInput = standaloneInput("newComment") {
            classes = "issue-page__prompt"
          }
          commentInput.placeholder = "Neuer Kommentar..."
          commentInput.readOnly = true
          commentInput.onClick(_ => appendNewComment(service))
        }
      }
    }
  }

  private def syncItems(): Unit =
    itemsProperty.setAll(
      Seq(new Data(model).asInstanceOf[Data[? <: AbstractEntity[?]]]) ++
        commentsProperty.iterator.map(comment => new Data(comment).asInstanceOf[Data[? <: AbstractEntity[?]]])
    )
}

object IssuePage {

  final class IssueViewIssueCard(issue: Issue, onPersist: () => Unit) extends DivComposite {

    override protected def compose(using DslContext): Unit = {
      classes = "issue-page__issue-card"

      withDslContext {
        form(issue) {
          classes = "issue-page__form"
          onSubmit = { (_: Form[Issue]) =>
            onPersist()
          }

          vbox {
            classes = "issue-page__editor-shell"

            hbox {
              classes = "issue-page__titlebar"

              div {
                classes = "issue-page__title-field"
                style {
                  flex = "1"
                }

                inputContainer("Titel") {
                  input("title") {
                    classes = "issue-page__title-input"
                    subscribeBidirectional(issue.title, stringValueProperty)
                    addDisposable(issue.editable.observe(editable => disabled = !editable))
                  }
                }
              }

              renderByRel("update", issue.links) { () =>
                button("edit") {
                  buttonType = "button"
                  classes = Seq("material-icons", "issue-page__icon-btn")
                  onClick { _ =>
                    issue.editable.set(!issue.editable.get)
                  }
                }
              }
            }

            editor("editor") {
              classes = "issue-page__editor"
              style {
                flex = "1"
                minHeight = "240px"
              }

              basePlugin {}
              headingPlugin {}
              listPlugin {}
              linkPlugin {}
              imagePlugin {}

              button("save") {
                classes = Seq("material-icons", "hover")
              }

              subscribeBidirectional(issue.editor, valueProperty)
              subscribeBidirectional(issue.editable, editableProperty)
            }
          }
        }
      }
    }
  }

  object IssueViewIssueCard {
    def card(issue: Issue, onPersist: () => Unit)(using Scope): IssueViewIssueCard =
      CompositeSupport.buildComposite(new IssueViewIssueCard(issue, onPersist))
  }

  def issuePage(model: Issue, init: IssuePage ?=> Unit = {})(using Scope): IssuePage =
    CompositeSupport.buildPage(new IssuePage(model))(init)
}
