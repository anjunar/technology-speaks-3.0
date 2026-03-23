package app.pages.documents

import app.components.commentable.FirstCommentCard.firstCommentCard
import app.components.shared.LoadingCard.loadingCard
import app.domain.core.{Data, Table}
import app.domain.documents.{Document, Issue, IssueCreated, IssueUpdated}
import app.domain.shared.FirstComment
import app.services.ApplicationService
import app.support.{Api, RemotePageQuery, RemoteTableList}
import app.ui.{CompositeSupport, DivComposite, PageComposite}
import jfx.action.Button.button
import jfx.action.Button.{buttonType_=, onClick}
import jfx.control.virtualList
import jfx.core.component.ElementComponent.*
import jfx.core.component.NodeComponent
import jfx.core.state.{ListProperty, Property, RemoteListProperty}
import jfx.dsl.*
import jfx.form.Editor.editor
import jfx.form.Form
import jfx.form.Form.{form, onSubmit_=}
import jfx.form.Input.input
import jfx.form.InputContainer.inputContainer
import jfx.form.editor.plugins.*
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.layout.VBox.vbox
import jfx.statement.DynamicOutlet.dynamicOutlet
import org.scalajs.dom.Node

import scala.concurrent.{ExecutionContext, Future}

class IssuePage extends PageComposite("Aufgabe") {

  private given ExecutionContext = ExecutionContext.global
  private val pageSize = 40

  private val modelProperty: Property[Data[Issue]] = Property(new Data[Issue](new Issue()))
  private val documentIdProperty: Property[String] = Property("")
  private val commentsProperty: RemoteListProperty[FirstComment, RemotePageQuery] =
    RemoteTableList.createMapped[Data[FirstComment], FirstComment](pageSize = pageSize) { (index, limit) =>
      val issue = modelProperty.get.data
      if (issue.id.get != null) {
        FirstComment.list(index, limit, issue)
      } else {
        Future.successful(new Table[Data[FirstComment]]())
      }
    }(_.data)
  private val contentProperty: Property[NodeComponent[? <: Node] | Null] = Property(null)

  def model(value: Data[Issue]): Unit = {
    if (Option(value).exists(_.data != null) && value.data.id.get == null) {
      value.data.editable.set(true)
    }

    modelProperty.set(value)
  }

  def documentId(value: String): Unit =
    documentIdProperty.set(Option(value).getOrElse(""))

  override protected def compose(using DslContext): Unit = {
    classProperty += "issue-page"

    addDisposable(
      modelProperty.observe { value =>
        contentProperty.set(IssuePageContent.content(value.data, commentsProperty, handleIssueSaved, appendNewComment, replaceComment, removeComment))
        reloadComments()
      }
    )

    withDslContext {
      dynamicOutlet(contentProperty)
    }
  }

  private def reloadComments(): Unit =
    if (modelProperty.get.data.id.get != null) {
      RemoteTableList.reloadFirstPage(commentsProperty, pageSize = pageSize)
    } else {
      commentsProperty.clear()
    }

  private def handleIssueSaved(linkRel: String, saved: Data[Issue]): Unit = {
    if (linkRel == "update") {
      ApplicationService.messageBus.publish(new IssueUpdated(saved))
    } else {
      ApplicationService.messageBus.publish(new IssueCreated(saved))
    }

    close()
  }

  private def appendNewComment(): Unit =
    if (!commentsProperty.lastOption.exists(_.editable.get)) {
      val comment = new FirstComment()
      comment.editable.set(true)
      comment.user.set(ApplicationService.app.get.user)
      commentsProperty += comment
    }

  private def replaceComment(previous: FirstComment, next: FirstComment): Unit = {
    val index = commentsProperty.indexWhere(_ eq previous)
    if (index >= 0) commentsProperty.update(index, next)
    else commentsProperty += next
  }

  private def removeComment(comment: FirstComment): Unit =
    commentsProperty.indexWhere(_ eq comment) match {
      case index if index >= 0 =>
        commentsProperty.remove(index)
      case _ =>
        ()
    }
}

object IssuePage {
  def issuePage(init: IssuePage ?=> Unit = {}): IssuePage =
    CompositeSupport.buildPage(new IssuePage)(init)
}

private final class IssuePageContent(
  issue: Issue,
  comments: ListProperty[FirstComment],
  onIssueSaved: (String, Data[Issue]) => Unit,
  appendNewComment: () => Unit,
  replaceComment: (FirstComment, FirstComment) => Unit,
  removeComment: FirstComment => Unit
) extends DivComposite {

  private given ExecutionContext = ExecutionContext.global

  override protected def compose(using DslContext): Unit = {
    withDslContext {
      vbox {
        style {
          rowGap = "12px"
          height = "100%"
          padding = "12px"
        }

        div {
          style {
            flex = "1"
            minHeight = "0px"
          }

          virtualList(comments, estimateHeightPx = 240, overscanPx = 240, prefetchItems = 40) { (comment, _) =>
            if (comment == null) {
              val card = loadingCard {}
              card.minHeight("160px")
              card
            } else {
              firstCommentCard(
                comment = comment,
                owner = issue,
                onPersist = saved => replaceComment(comment, saved),
                onDeleteCompleted = saved => removeComment(saved)
              )
            }
          }
        }

        val prompt = input("newComment") {
          style {
            padding = "12px"
            width = "calc(100% - 24px)"
            backgroundColor = "var(--color-background-secondary)"
            fontSize = "24px"
            borderRadius = "8px"
          }
        }

        prompt.placeholder = "Neuer Kommentar..."
        prompt.element.readOnly = true
        prompt.element.onclick = _ => appendNewComment()
        prompt.element.style.display =
          if (issue.id.get != null) "block" else "none"
      }
    }
  }
}

private object IssuePageContent {
  def content(
    issue: Issue,
    comments: ListProperty[FirstComment],
    onIssueSaved: (String, Data[Issue]) => Unit,
    appendNewComment: () => Unit,
    replaceComment: (FirstComment, FirstComment) => Unit,
    removeComment: FirstComment => Unit
  ): IssuePageContent =
    CompositeSupport.buildComposite(new IssuePageContent(issue, comments, onIssueSaved, appendNewComment, replaceComment, removeComment))
}

private final class IssueEditorCard(
  issue: Issue,
  onIssueSaved: (String, Data[Issue]) => Unit
) extends DivComposite {

  private given ExecutionContext = ExecutionContext.global

  override protected def compose(using DslContext): Unit = {
    classProperty += "glass-border"

    withDslContext {
      form(issue) {
        onSubmit_= { (event : Form[Issue])  =>
          issue.links.find(link => link.rel == "update" || link.rel == "save").foreach { link =>
            Api.invokeLink[Data[Issue]](link, issue).foreach(saved => onIssueSaved(link.rel, saved))
          }
        }

        vbox {
          style {
            rowGap = "10px"
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
                val titleInput = input("title") {}
                addDisposable(issue.editable.observe(editable => titleInput.element.disabled = !editable))
              }
            }

            val editButton = button("edit") {
              buttonType_=("button")
              classes = "material-icons"
              onClick { _ =>
                issue.editable.set(!issue.editable.get)
              }
            }

            editButton.element.style.display =
              if (issue.links.exists(link => link.rel == "update")) "inline-flex" else "none"
          }

          val editorField = editor("editor") {
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
          }

          addDisposable(Property.subscribeBidirectional(issue.editable, editorField.editableProperty))
        }
      }
    }
  }
}

private object IssueEditorCard {
  def card(issue: Issue, onIssueSaved: (String, Data[Issue]) => Unit): IssueEditorCard =
    CompositeSupport.buildComposite(new IssueEditorCard(issue, onIssueSaved))
}
