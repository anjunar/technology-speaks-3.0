package app.components.likeable

import app.domain.core.Link
import app.domain.shared.Like
import app.services.ApplicationService
import jFx2.client.JsonClient
import jFx2.controls.button
import jFx2.controls.text
import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dom.ElementInsertPoint
import jFx2.core.dsl.className
import jFx2.core.dsl.onClick
import jFx2.core.dsl.style
import jFx2.core.template
import jFx2.layout.hbox
import jFx2.state.JobRegistry
import jFx2.state.ListProperty
import jFx2.state.Property
import org.w3c.dom.HTMLDivElement
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit

@JfxComponentBuilder(classes = ["like-button"])
class LikeButton(override val node: HTMLDivElement) : Component<HTMLDivElement>() {

    private var likes: ListProperty<Like>? = null
    private var links: ListProperty<Link>? = null
    private var rel: String = "like"

    private val liked = Property(false)
    private val count = Property(0)
    private val busy = Property(false)

    fun model(
        likes: ListProperty<Like>,
        links: ListProperty<Link>,
        rel: String = "like"
    ) {
        this.likes = likes
        this.links = links
        this.rel = rel
    }

    context(scope: NodeScope)
    fun afterBuild() {
        val likes = likes ?: return
        val links = links ?: return

        fun recompute() {
            val userId = ApplicationService.app.get().user.id?.get()
            val currentLikes = likes.get()
            count.set(currentLikes.size)
            liked.set(userId != null && currentLikes.any { it.user.id?.get() == userId })
        }

        onDispose(likes.observe { recompute() })
        onDispose(ApplicationService.app.observe { recompute() })

        template {
            hbox {
                style {
                    alignItems = "center"
                    columnGap = "6px"
                }

                val icon = button("favorite_border") {
                    type("button")
                    className { "material-icons container hover" }

                    onClick {
                        val link = links.get().firstOrNull { it.rel == this@LikeButton.rel } ?: return@onClick
                        if (busy.get()) return@onClick

                        JobRegistry.instance.launch("Like", owner = this@LikeButton) {
                            busy.set(true)
                            try {
                                val headers = Headers()
                                headers.set("Accept", "application/json")

                                val updated = JsonClient.invoke<List<Like>>(
                                    if (link.url.startsWith("/service/")) link.url else "/service${link.url}",
                                    RequestInit(
                                        method = link.method,
                                        headers = headers
                                    )
                                )

                                likes.setAll(updated)
                            } finally {
                                busy.set(false)
                            }
                        }
                    }
                }

                onDispose(busy.observe { icon.node.disabled = it })
                onDispose(liked.observe { isLiked ->
                    icon.node.classList.toggle("active", isLiked)
                    icon.node.textContent = if (isLiked) "favorite" else "favorite_border"
                })

                text(count) { v -> v.toString() }
            }
        }
    }
}