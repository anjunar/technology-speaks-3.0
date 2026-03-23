package app

import app.domain.core.User
import app.domain.documents.{Document, Issue}
import app.domain.timeline.Post
import app.pages.HomePage.homePage
import app.pages.core.{UserPage, UsersPage}
import app.pages.documents.{DocumentPage, IssuePage}
import app.pages.followers.RelationShipsPage.relationShipsPage
import app.pages.security.*
import app.pages.timeline.{PostEditPage, PostViewPage, PostsPage}
import jfx.dsl.Scope
import jfx.router.{Route, RouteContext}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

object Routes {

  private def route(path: String)(factory: Route.ScopedFactory): Route =
    Route.scoped(path, factory)

  private def asyncRoute(path: String)(factory: RouteContext ?=> Future[Route.Component]): Route =
    Route.scopedPromise(
      path,
      (context: RouteContext) ?=>
        (scope: Scope) ?=>
          factory(using context).toJSPromise
    )

  private def pathParam(name: String)(using context: RouteContext): String =
    context.pathParams.get(name).getOrElse("")

  val routes: js.Array[Route] = js.Array(
    route("/") {
      homePage()
    },
    asyncRoute("/document/documents/document/root") {
      Document.root().map { root =>
        val page = DocumentPage.documentPage()
        page.model(root)
        page
      }
    },
    asyncRoute("/document/documents/document/:documentId/issues/issue") {
      val docId = pathParam("documentId")
      Issue.read(docId).map { issue =>
        val page = IssuePage.issuePage()
        page.documentId(docId)
        page.model(issue)
        page
      }
    },
    asyncRoute("/document/documents/document/:documentId/issues/issue/:id") {
      val docId = pathParam("documentId")
      val issueId = pathParam("id")
      Issue.read(docId, issueId).map { issue =>
        val page = IssuePage.issuePage()
        page.documentId(docId)
        page.model(issue)
        page
      }
    },
    route("/followers/relationships") {
      relationShipsPage()
    },
    route("/timeline/posts") {
      PostsPage.postsPage()
    },
    route("/timeline/posts/post") {
      PostEditPage.postEditPage(new Post())
    },
    asyncRoute("/timeline/posts/post/:id") {
      val id = pathParam("id")
      app.domain.timeline.Post.read(id).map { post =>
        PostEditPage.postEditPage(post.data)
      }
    },
    asyncRoute("/timeline/posts/post/:id/view") {
      val id = pathParam("id")
      app.domain.timeline.Post.read(id).map { post =>
        PostViewPage.postViewPage(post.data)
      }
    },
    route("/security/login") {
      PasswordLoginPage.passwordLoginPage()
    },
    route("/security/register") {
      PasswordRegisterPage.passwordRegisterPage()
    },
    route("/security/login/options") {
      WebAuthnLoginPage.webAuthnLoginPage()
    },
    route("/security/register/options") {
      WebAuthnRegisterPage.webAuthnRegisterPage()
    },
    route("/security/logout") {
      LogoutPage.logoutPage()
    },
    route("/security/confirm") {
      ConfirmPage.confirmPage()
    },
    route("/core/users") {
      UsersPage.usersPage()
    },
    asyncRoute("/core/users/user/:id") {
      val id = pathParam("id")
      User.read(id).map { user =>
        UserPage.userPage(user.data)
      }
    }
  )
}
