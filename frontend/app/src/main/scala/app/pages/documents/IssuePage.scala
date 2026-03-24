package app.pages.documents

import app.components.commentable.FirstCommentCard.firstCommentCard
import app.components.shared.LoadingCard.loadingCard
import app.domain.core.{Data, Table}
import app.domain.documents.{Issue, IssueCreated, IssueUpdated}
import app.domain.shared.FirstComment
import app.services.ApplicationService
import app.support.Navigation.renderByRel
import app.support.{Api, RemotePageQuery, RemoteTableList}
import app.ui.{CompositeSupport, PageComposite}
import jfx.action.Button.{button, buttonType, buttonType_=, onClick}
import jfx.control.virtualList
import jfx.core.component.ElementComponent.*
import jfx.core.state.Property.subscribeBidirectional
import jfx.core.state.{Property, RemoteListProperty}
import jfx.dsl.*
import jfx.form.Control.valueProperty
import jfx.form.Editable.editableProperty
import jfx.form.Editor.editor
import jfx.form.{ErrorResponseException, Form}
import jfx.form.Form.{form, onSubmit, onSubmit_=}
import jfx.form.Input.{disabled, input, stringValueProperty}
import jfx.form.InputContainer.inputContainer
import jfx.form.editor.plugins.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox
import jfx.layout.Viewport

import scala.concurrent.{ExecutionContext, Future}

class IssuePage(val model: Issue) extends PageComposite("Aufgabe", pageResizable = true) {

  private given ExecutionContext = ExecutionContext.global
  private val pageSize = 50

  private val commentsProperty: RemoteListProperty[FirstComment, RemotePageQuery] =
    RemoteTableList.createMapped[Data[FirstComment], FirstComment](pageSize = pageSize) { (index, limit) =>
      if (model.id.get != null) {
        FirstComment.list(index, limit, model)
      } else {
        Future.successful(new Table[Data[FirstComment]]())
      }
    }(_.data)

  private def persistIssue(service: ApplicationService): Unit = {
    try {
      model.links.find(link => link.rel == "update" || link.rel == "save").foreach { link =>
        Api.invokeLink[Data[Issue]](link, model).foreach { saved =>
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
  }

  private def removeComment(comment: FirstComment): Unit = {
    val index = commentsProperty.indexOf(comment)
    if (index >= 0) commentsProperty.remove(index)
  }

  private def appendNewComment(service: ApplicationService): Unit = {
    if (!commentsProperty.lastOption.exists(_.editable.get)) {
      val comment = new FirstComment()
      comment.editable.set(true)
      comment.user.set(service.app.get.user)
      commentsProperty += comment
    }
  }

  override protected def compose(using DslContext): Unit = {
    classProperty += "issue-page"

    withDslContext {

      val service = inject[ApplicationService]

      form(model) {
          onSubmit = { (_: Form[Issue]) =>
            persistIssue(service)
          }

          vbox {
            style {
              padding = "10px"
              rowGap = "10px"
              height = "100%"
              flex = "1"
            }

            hbox {
              style {
                alignItems = "flex-start"
                columnGap = "10px"
              }

              div {
                style {
                  flex = "1"
                }

                inputContainer("Titel") {
                  val titleInput = input("title") {
                    subscribeBidirectional(model.title, stringValueProperty)
                    addDisposable(model.editable.observe(editable => disabled = !editable))
                  }
                }
              }

              renderByRel("update", model.links) { () =>
                button("edit") {
                  buttonType = "button"
                  classes = "material-icons"
                  onClick { _ =>
                    model.editable.set(!model.editable.get)
                  }
                }
              }
            }

            editor("editor") {
              style {
                flex = "1"
                minHeight = "240px"
              }

              basePlugin {}
              headingPlugin {}
              listPlugin {}
              linkPlugin {}
              imagePlugin {}

              subscribeBidirectional(model.editor, valueProperty)
              subscribeBidirectional(model.editable, editableProperty)

            }

            hbox {
              style {
                justifyContent = "flex-end"
                columnGap = "10px"
              }
              renderByRel("update", model.links) { () =>
                button("Aktualisieren")
              }
              renderByRel("save", model.links) { () =>
                button("Speichern")
              }
            }
          }

          div {
              style {
                flex = "1"
                minHeight = "0px"
                marginTop = "10px"
              }

              virtualList(commentsProperty, estimateHeightPx = 240, prefetchItems = 40) { (comment, _) =>
                if (comment == null) {
                  loadingCard {
                    summon[app.components.shared.LoadingCard].cardMinHeight("160px")
                  }
                } else {
                  firstCommentCard(
                    comment = comment,
                    owner = model,
                    onPersist = saved => upsertComment(comment, saved),
                    onDeleteCompleted = _ => removeComment(comment)
                  )
                }
              }
            }

            renderByRel("save", model.links) { () =>
              val commentInput = input("newComment") {
                style {
                  padding = "12px"
                  width = "calc(100% - 24px)"
                  backgroundColor = "var(--color-background-secondary)"
                  fontSize = "24px"
                  borderRadius = "8px"
                }
              }
              commentInput.placeholder = "Neuer Kommentar..."
              commentInput.readOnly = true
              commentInput.onClick(_ => appendNewComment(service))
            }
          }
      }
  }
}

object IssuePage {
  def issuePage(model: Issue, init: IssuePage ?=> Unit = {}): IssuePage =
    CompositeSupport.buildPage(new IssuePage(model))(init)
}
