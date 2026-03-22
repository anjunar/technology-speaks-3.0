package app

import app.domain.core.User
import app.domain.documents.Document
import app.domain.documents.Issue
import app.domain.followers.RelationShip
import app.domain.timeline.Post
import app.pages.core.userPage
import app.pages.core.usersPage
import app.pages.documents.documentPage
import app.pages.documents.issuePage
import app.pages.followers.relationShipsPage
import app.pages.homePage
import app.pages.security.*
import app.pages.timeline.postEditPage
import app.pages.timeline.postViewPage
import app.pages.timeline.postsPage
import jFx2.router.Route

object Routes {

    val routes = listOf(
        Route(
            path = "/",
            factory = { homePage {} },
            children = listOf(
                Route(
                    path = "document",
                    children = listOf(
                        Route(
                            path = "documents",
                            children = listOf(
                                Route(
                                    path = "document",
                                    children = listOf(
                                        Route(
                                            path = "root",
                                            factory = {

                                                val root = Document.root()

                                                val documents = Document.list(0, 200)

                                                documentPage {
                                                    model(root.data, documents)
                                                }
                                            }
                                        )
                                    )
                                ),
                                Route(
                                    path = "document/:documentId",
                                    children = listOf(
                                        Route(
                                            path = "issues",
                                            children = listOf(
                                                Route(
                                                    path = "issue",
                                                    factory = { params ->
                                                        val issue = Issue.read(params["documentId"]!!)

                                                        issuePage {
                                                            documentId(params["documentId"]!!)
                                                            model(issue.data)
                                                        }
                                                    }
                                                ),
                                                Route(
                                                    path = "issue/:id",
                                                    factory = { params ->
                                                        val issue = Issue.read(params["documentId"]!!, params["id"]!!)

                                                        issuePage {
                                                            documentId(params["documentId"]!!)
                                                            model(issue.data)
                                                        }
                                                    }
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                ),
                Route(
                    path = "followers",
                    children = listOf(
                        Route(
                            path = "relationships",
                            factory = {
                                val table = RelationShip.list(0, 200)

                                relationShipsPage {
                                    model(table)
                                }
                            }
                        )
                    )
                ),
                Route(
                    path = "timeline",
                    children = listOf(
                        Route(
                            path = "posts",
                            factory = {
                                val table = Post.list(0, 200)
                                postsPage {
                                    model(table)
                                }
                            },
                            children = listOf(
                                Route(
                                    path = "post",
                                    factory = {
                                        postEditPage {}
                                    }
                                ),
                                Route(
                                    path = "post/:id",
                                    factory = { params ->
                                        val post = Post.read(params["id"]!!)

                                        postEditPage {
                                            model(post)
                                        }
                                    }
                                ),
                                Route(
                                    path = "post/:id/view",
                                    factory = { params ->
                                        val post = Post.read(params["id"]!!)

                                        postViewPage {
                                            model(post)
                                        }
                                    }
                                )
                            )
                        )
                    )
                ),
                Route(
                    path = "security",
                    children = listOf(
                        Route(path = "login", factory = { passwordLoginPage {} }),
                        Route(path = "register", factory = { passwordRegisterPage {} }),
                        Route(path = "login/options", factory = { webAuthnLoginPage {} }),
                        Route(path = "register/options", factory = { webAuthnRegisterPage {} }),
                        Route(path = "logout", factory = { logoutPage {} }),
                        Route(path = "confirm", factory = { confirmPage { } })
                    )
                ),
                Route(
                    path = "core",
                    children = listOf(
                        Route(
                            path = "users",
                            factory = {
                                val table = User.list(0, 50)

                                usersPage {
                                    model(table)
                                }
                            },
                            children = listOf(
                                Route(
                                    path = "user/:id",
                                    factory = { params ->
                                        val user = User.read(params["id"]!!)

                                        userPage {
                                            model(user)
                                        }
                                    }
                                )
                            )
                        )
                    )
                )
            )
        )
    )

}
