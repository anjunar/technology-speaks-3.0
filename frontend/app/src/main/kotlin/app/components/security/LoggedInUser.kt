package app.components.security

import app.services.ApplicationService
import jFx2.controls.image
import jFx2.controls.link
import jFx2.controls.text
import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dsl.className
import jFx2.core.dsl.style
import jFx2.core.rendering.observeRender
import jFx2.core.template
import jFx2.layout.div
import jFx2.layout.hbox
import org.w3c.dom.HTMLDivElement

@JfxComponentBuilder(classes = ["logged-in-user"])
class LoggedInUser(override val node: HTMLDivElement) : Component<HTMLDivElement>() {

    context(scope: NodeScope)
    fun afterBuild() {

        val model = ApplicationService.app

        template {

            observeRender(model) { model ->

                hbox {

                    style {
                        alignItems = "center"
                        columnGap = "10px"
                    }

                    link("/core/users/user/${model.user.id!!.get()}") {
                        if (model.user.image.get() == null) {
                            div {
                                style {
                                    fontSize = "32px"
                                }
                                className { "material-icons" }
                                text("account_circle")
                            }
                        } else {
                            image {
                                style {
                                    height = "32px"
                                    width = "32px"
                                }
                                src = model.user.image.get()?.thumbnailLink()!!
                            }
                        }
                    }

                    div {
                        if (model.user.info.get() != null) {
                            text(model.user.info.get()!!.firstName.get() + " " + model.user.info.get()!!.lastName.get())
                        } else {
                            text(model.user.nickName.get())
                        }

                    }

                }


            }
        }

    }

}
