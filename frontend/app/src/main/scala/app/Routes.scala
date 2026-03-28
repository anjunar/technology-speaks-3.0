package app

import app.domain.core.{Data, Link, User}
import app.domain.documents.{Document, Issue}
import app.domain.followers.{Group, RelationShip}
import app.domain.security.Account
import app.domain.timeline.Post
import app.pages.HomePage.homePage
import app.pages.core.{UserPage, UsersPage}
import app.pages.documents.{DocumentPage, IssuePage}
import app.pages.followers.{GroupsPage, RelationShipsPage}
import app.pages.security.*
import app.pages.timeline.{PostEditPage, PostViewPage, PostsPage}
import app.support.{RemotePageQuery, RemoteTableList}
import jfx.core.state.{ListProperty, Property, RemoteListProperty}
import jfx.dsl.{DslRuntime, Scope}
import jfx.router.{Route, RouteContext}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

object Routes {

  private def route(path: String)(factory: Route.ScopedFactory): Route =
    Route.scoped(path, factory)

  private def asyncRoute(path: String)(factory: (RouteContext, Scope) ?=> Future[Route.Component]): Route =
    Route.scopedPromise(
      path,
      (context: RouteContext) ?=>
        (scope: Scope) ?=>
          factory(using context, scope).toJSPromise
    )

  private def pathParam(name: String)(using context: RouteContext): String =
    context.pathParams.get(name).getOrElse("")

  val routes: js.Array[Route] = js.Array(
    route("/") {
      homePage()
    },
    asyncRoute("/document/documents/document/root") {
      Document.root().map { root =>
        DocumentPage.documentPage(root.data)
      }
    },
    asyncRoute("/document/documents/document/:id") {
      val id = pathParam("id")
      Document.read(id).map { document =>
        DocumentPage.documentPage(document.data)
      }
    },
    asyncRoute("/document/documents/document/:documentId/issues/issue") {
      val docId = pathParam("documentId")
      Issue.read(docId).map { issue =>
        issue.data.editable.set(true)
        IssuePage.issuePage(issue.data)
      }
    },
    asyncRoute("/document/documents/document/:documentId/issues/issue/:id") {
      val docId = pathParam("documentId")
      val issueId = pathParam("id")
      Issue.read(docId, issueId).map { issue =>
        IssuePage.issuePage(issue.data)
      }
    },
    asyncRoute("/followers/relationships") {
      val searchQueryProperty = Property("")
      val selectedGroupsProperty = ListProperty[Group]()
      val relationShipsProperty: RemoteListProperty[Data[RelationShip], RemotePageQuery] =
        RemoteTableList.create[Data[RelationShip]](pageSize = 200) { query =>
          RelationShip.list(
            query.index,
            query.limit,
            query = searchQueryProperty.get,
            groups = selectedGroupsProperty.iterator.toSeq,
            sorting = query.effectiveSortSpecs(Seq("created:desc"))
          )
        }

      relationShipsProperty.reload(RemotePageQuery.first(200)).toFuture.map(_ => {
        RelationShipsPage.relationShipsPage(relationShipsProperty, searchQueryProperty, selectedGroupsProperty) {}
      })
    },
    route("/followers/groups") {
      GroupsPage.groupsPage()
    },
    asyncRoute("/timeline/posts") {
      val postsProperty: RemoteListProperty[Data[Post], RemotePageQuery] =
        RemoteTableList.create[Data[Post]](pageSize = 50) { query =>
          Post.list(query.index, query.limit)
        }

      postsProperty.reload(RemotePageQuery.first(50)).toFuture.map(_ => {
        PostsPage.postsPage(postsProperty) {}
      })
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
    asyncRoute("/security/account") {
      Account.read().map { account =>
        AccountPage.accountPage(account)
      }
    },
    route("/security/confirm") {
      ConfirmPage.confirmPage()
    },
    asyncRoute("/core/users") {
      val searchQueryProperty = Property("")
      val usersProperty: RemoteListProperty[Data[User], RemotePageQuery] =
        RemoteTableList.create[Data[User]](pageSize = 50) { query =>
          User.list(query.index, query.limit, searchQueryProperty.get, sorting = query.effectiveSortSpecs(Seq("created:desc")))
        }

      usersProperty.reload(RemotePageQuery.first(50)).toFuture.map(_ => {
        UsersPage.usersPage(usersProperty, searchQueryProperty)
      })
    },
    asyncRoute("/core/users/user/:id") {
      val id = pathParam("id")
      User.read(id).map { user =>
        UserPage.userPage(user)
      }
    }
  )
}
