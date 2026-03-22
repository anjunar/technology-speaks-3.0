package app.pages.security

import app.domain.security.PasswordRegister
import jFx2.client.JsonClient
import jFx2.client.JsonResponse
import jFx2.controls.button
import jFx2.controls.heading
import jFx2.controls.image
import jFx2.controls.text
import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dom.ElementInsertPoint
import jFx2.core.dsl.className
import jFx2.core.dsl.onClick
import jFx2.core.dsl.style
import jFx2.core.dsl.subscribeBidirectional
import jFx2.core.template
import jFx2.forms.EmailValidator
import jFx2.forms.SizeValidator
import jFx2.forms.form
import jFx2.forms.input
import jFx2.forms.inputContainer
import jFx2.layout.div
import jFx2.layout.hbox
import jFx2.router.PageInfo
import jFx2.state.JobRegistry
import kotlinx.browser.window
import org.w3c.dom.HTMLDivElement

@JfxComponentBuilder(classes = ["password-register-page"])
class PasswordRegisterPage(override val node: HTMLDivElement) : Component<HTMLDivElement>(), PageInfo {

    override val name: String = "Register"
    override val width: Int = -1
    override val height: Int = -1
    override val resizable: Boolean = false
    override var close: () -> Unit = {}

    context(scope: NodeScope)
    fun afterBuild() {

        val registerForm = PasswordRegister()

        template {
            form(model = registerForm, clazz = PasswordRegister::class) {

                onSubmit {

                    model.save()

                    close()

                }

                image {
                    style {
                        width = "500px"
                    }

                    src = "/app/security/register.png"
                }

                hbox {

                    style {
                        justifyContent = "center"
                    }

                    heading(3) {
                        text { "Möchtest du dich Registrieren?" }
                    }
                }

                div {

                    style {
                        padding = "20px"
                    }

                    inputContainer("Nick name") {

                        input("nickName") {
                            validatorsProperty.add(SizeValidator(3, 30))
                            subscribeBidirectional(this@form.model.nickName, valueProperty)
                        }

                    }

                    inputContainer("Email") {

                        input("email") {
                            type("email")
                            validatorsProperty.add(EmailValidator())
                            subscribeBidirectional(this@form.model.email, valueProperty)
                        }

                    }

                    inputContainer("Password") {

                        input("password") {
                            type("password")
                            validatorsProperty.add(SizeValidator(5, 30))
                            subscribeBidirectional(this@form.model.password, valueProperty)
                        }

                    }
                }

                div {
                    className { "button-container" }

                    button("Abbrechen") {
                        onClick {
                            close()
                        }
                        className { "btn-secondary" }
                    }

                    button("Registrieren") {
                        className { "btn-danger" }
                    }
                }

            }
        }



    }

}