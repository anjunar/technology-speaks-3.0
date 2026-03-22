@file:OptIn(ExperimentalUuidApi::class)

package jFx2.modals

import jFx2.controls.text
import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dsl.className
import jFx2.core.dsl.onClick
import jFx2.core.dsl.renderComponent
import jFx2.core.dsl.renderFields
import jFx2.core.dsl.style
import jFx2.core.dsl.subscribeBidirectional
import jFx2.core.rendering.foreach
import jFx2.core.template
import jFx2.layout.div
import jFx2.router.PageInfo
import jFx2.state.Disposable
import jFx2.state.ListProperty
import jFx2.state.Property
import kotlinx.browser.window
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@JfxComponentBuilder(classes = ["view-port"])
class Viewport(override val node: HTMLDivElement) : Component<HTMLDivElement>() {

    context(scope: NodeScope)
    fun afterBuild() {

        template {
            renderFields(*this@Viewport.children.toTypedArray())

            foreach(windows, { key -> key.id }) { window, index ->

                window {

                    subscribeBidirectional(window.zIndex, zIndex)
                    subscribeBidirectional(window.maximized, maximized)

                    title = window.title
                    resizeable = window.resizable

                    onCloseWindow {
                        closeWindow(window)
                    }

                    onClickWindow {
                        window.onClick?.invoke(it)
                        touchWindow(window)
                    }

                    div {
                        style {
                            width = "100%"
                            height = "100%"
                        }
                        val field = window.component()

                        if (field is PageInfo) {
                            (field as PageInfo).close = { closeWindowById(window.id) }
                        }

                        renderComponent(field)
                    }
                }
            }

            foreach(overlays, { key -> key.id }) { overlay, index ->
                div {

                    className { "jfx2-overlay" }

                    onClick { it.stopPropagation() }

                    val overlayElement = node as HTMLElement
                    val follow = followAnchorFixed(
                        overlayElement = overlayElement,
                        anchorElement = overlay.anchor,
                        offsetXPx = overlay.offsetXPx,
                        offsetYPx = overlay.offsetYPx,
                        widthPx = overlay.widthPx,
                        minWidthPx = overlay.minWidthPx,
                        maxHeightPx = overlay.maxHeightPx,
                        marginViewportPx = overlay.marginViewportPx,
                        flipY = overlay.flipY,
                    )
                    onDispose(follow)

                    style {
                        zIndex = overlay.zIndex.toString()
                    }

                    overlay.content()
                }
            }

            div {
                className { "jfx2-notification-host" }

                foreach(notifications, { key -> key.id }) { notification, index ->
                    div {
                        className { "jfx2-notification kind-${notification.kind.cssClass}" }

                        onDispose(notification.visible.observe { visible ->
                            if (visible) {
                                node.classList.remove("is-hidden")
                            } else {
                                node.classList.add("is-hidden")
                            }
                        })

                        onClick {
                            closeNotification(notification)
                        }

                        text(notification.message)
                    }
                }
            }
        }


    }

    companion object {
        val windows = ListProperty<WindowConf>()
        val notifications = ListProperty<NotificationConf>()
        val overlays = ListProperty<OverlayConf>()

        private const val notificationFadeOutMs: Int = 250

        enum class NotificationKind(val cssClass: String) {
            INFO("info"),
            SUCCESS("success"),
            WARNING("warning"),
            ERROR("error")
        }

        class NotificationConf(
            val message: String,
            val kind: NotificationKind = NotificationKind.INFO,
        ) {
            val id: String = Uuid.generateV4().toString()
            val visible: Property<Boolean> = Property(true)
        }

        class WindowConf(
            val title : String,
            val component : context(NodeScope) () -> Component<*>,
            val zIndex : Property<Int> = Property(0),
            val onClose: ((Window) -> Unit)? = null,
            val onClick: ((Window) -> Unit)? = null,
            val maximized: Property<Boolean> = Property(false),
            val resizable: Boolean = false
        ) {
            val id : String = Uuid.generateV4().toString()
        }

        class OverlayConf(
            val id: String = Uuid.generateV4().toString(),
            val anchor: HTMLElement,
            val offsetXPx: Double = 0.0,
            val offsetYPx: Double = 0.0,
            val widthPx: Double? = null,
            val minWidthPx: Double? = null,
            val maxHeightPx: Double? = null,
            val marginViewportPx: Double = 8.0,
            val flipY: Boolean = true,
            val zIndex: Int = 90000,
            val content: context(NodeScope) () -> Unit,
        )

        fun addOverlay(conf: OverlayConf) {
            overlays.add(conf)
        }

        fun closeOverlay(conf: OverlayConf) {
            overlays.remove(conf)
        }

        fun closeOverlayById(id: String) {
            overlays.firstOrNull { it.id == id }?.let { overlays.remove(it) }
        }

        fun notify(
            message: String,
            kind: NotificationKind = NotificationKind.INFO,
            durationMs: Int = 3000,
        ): NotificationConf {
            val conf = NotificationConf(message = message, kind = kind)
            notifications.add(conf)

            window.setTimeout({ conf.visible.set(false) }, durationMs)
            window.setTimeout({ notifications.remove(conf) }, durationMs + notificationFadeOutMs)

            return conf
        }

        fun closeNotification(conf: NotificationConf) {
            conf.visible.set(false)
            window.setTimeout({ notifications.remove(conf) }, notificationFadeOutMs)
        }

        fun isActive(conf: WindowConf) = windows.all { (it == conf) || (it.zIndex.get() < conf.zIndex.get()) }

        fun touchWindow(conf: WindowConf) {
            var index = 0
            windows.forEach { window -> window.zIndex.set(index++) }
            conf.zIndex.set(index)
            conf.maximized.set(true)
        }

        fun addWindow(conf: WindowConf) {
            windows.add(conf)
            var index = 0
            windows.forEach { window -> window.zIndex.set(index++) }
            conf.zIndex.set(index)
        }

        fun closeWindow(conf: WindowConf) {
            conf.maximized.set(false)
            window.setTimeout({
                windows.remove(conf)
            }, 300)
        }

        fun closeWindowById(id: String) {
            windows.firstOrNull { it.id == id }?.let {
                windows.remove(it)
            }
        }
    }

}

private fun followAnchorFixed(
    overlayElement: HTMLElement,
    anchorElement: HTMLElement,
    offsetXPx: Double,
    offsetYPx: Double,
    widthPx: Double?,
    minWidthPx: Double?,
    maxHeightPx: Double?,
    marginViewportPx: Double,
    flipY: Boolean,
): Disposable {
    var disposed = false
    var rafId: Int? = null

    fun apply() {
        if (disposed) return

        val anchorRect = anchorElement.getBoundingClientRect()
        val viewportWidth = window.innerWidth.toDouble()
        val viewportHeight = window.innerHeight.toDouble()

        val resolvedWidth = widthPx ?: anchorRect.width

        val desiredLeft = anchorRect.left + offsetXPx
        val minLeft = marginViewportPx
        val maxLeft = viewportWidth - resolvedWidth - marginViewportPx
        val left = if (maxLeft <= minLeft) minLeft else desiredLeft.coerceIn(minLeft, maxLeft)

        val measuredOverlayHeight = overlayElement.offsetHeight.toDouble().takeIf { it > 0 } ?: 0.0

        val belowTop = anchorRect.bottom + offsetYPx
        val aboveTop = anchorRect.top - measuredOverlayHeight - offsetYPx

        var top = belowTop

        if (flipY && measuredOverlayHeight > 0) {
            val spaceBelow = viewportHeight - belowTop - marginViewportPx
            val spaceAbove = anchorRect.top - marginViewportPx

            if (spaceBelow < measuredOverlayHeight && spaceAbove > spaceBelow) {
                top = aboveTop
            }
        }

        val desiredTop = top
        val minTop = marginViewportPx
        val maxTop = viewportHeight - marginViewportPx
        top = if (maxTop <= minTop) minTop else desiredTop.coerceIn(minTop, maxTop)

        overlayElement.style.left = "${left}px"
        overlayElement.style.top = "${top}px"

        if (widthPx != null) {
            overlayElement.style.width = "${resolvedWidth}px"
        } else {
            overlayElement.style.minWidth = "${resolvedWidth}px"
        }

        minWidthPx?.let { overlayElement.style.minWidth = "${it}px" }
        maxHeightPx?.let { overlayElement.style.maxHeight = "${it}px" }

        rafId = window.requestAnimationFrame { apply() }
    }

    rafId = window.requestAnimationFrame { apply() }

    return Disposable {
        disposed = true
        rafId.let { window.cancelAnimationFrame(it) }
    }
}
