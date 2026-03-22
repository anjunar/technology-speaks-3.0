@file:OptIn(ExperimentalUuidApi::class)

package jFx2.forms

import app.domain.core.Media
import app.domain.core.Thumbnail
import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.modals.Viewport
import jFx2.state.Property
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@JfxComponentBuilder(classes = ["image-cropper-dialog"])
class ImageCropperDialog(
    override val node: HTMLDivElement,
    private val field: ImageCropper,
    private val windowConf: Viewport.Companion.WindowConf,
    private val source: Media,
    private val session: ImageCropperSession,
) : Component<HTMLDivElement>() {

    private var previewScale: Double = 1.0
    private var loadedImage: HTMLImageElement? = null

    private var crop: CropRect? = null
    private var drag: DragState? = null

    private var livePending = false
    private val outCanvas: HTMLCanvasElement = document.createElement("canvas").unsafeCast<HTMLCanvasElement>()

    private lateinit var canvas: HTMLCanvasElement
    private lateinit var applyBtn: HTMLButtonElement
    private lateinit var resetBtn: HTMLButtonElement
    private lateinit var closeBtn: HTMLButtonElement

    context(scope: NodeScope)
    fun afterBuild() {
        node.classList.add("image-cropper")
        node.classList.add("image-cropper-dialog")

        val toolbar = scope.create<HTMLDivElement>("div").also { it.classList.add("toolbar") }

        applyBtn = scope.create<HTMLButtonElement>("button").also {
            it.type = "button"
            it.textContent = "Apply"
            it.classList.add("hover")
        }

        resetBtn = scope.create<HTMLButtonElement>("button").also {
            it.type = "button"
            it.textContent = "Reset"
            it.classList.add("hover")
        }

        closeBtn = scope.create<HTMLButtonElement>("button").also {
            it.type = "button"
            it.textContent = "Close"
            it.classList.add("hover")
        }

        toolbar.appendChild(applyBtn)
        toolbar.appendChild(resetBtn)
        toolbar.appendChild(closeBtn)

        val canvasWrap = scope.create<HTMLDivElement>("div").also { it.classList.add("canvas-wrap") }
        canvas = scope.create<HTMLCanvasElement>("canvas").also {
            it.classList.add("canvas")
            it.width = 1
            it.height = 1
        }
        canvasWrap.appendChild(canvas)

        node.appendChild(toolbar)
        node.appendChild(canvasWrap)

        wireCanvasDragging()

        val onApplyClick: (Event) -> Unit = onApplyClick@{
            val media = cropToMedia() ?: return@onApplyClick
            session.applied = true
            field.valueProperty.set(media)
            Viewport.closeWindow(windowConf)
        }
        applyBtn.addEventListener("click", onApplyClick)
        onDispose { applyBtn.removeEventListener("click", onApplyClick) }

        val onResetClick: (Event) -> Unit = {
            crop = defaultCrop()
            render()
            scheduleLivePreview()
        }
        resetBtn.addEventListener("click", onResetClick)
        onDispose { resetBtn.removeEventListener("click", onResetClick) }

        val onCloseClick: (Event) -> Unit = {
            Viewport.closeWindow(windowConf)
        }
        closeBtn.addEventListener("click", onCloseClick)
        onDispose { closeBtn.removeEventListener("click", onCloseClick) }

        // Load source image
        val img = document.createElement("img").unsafeCast<HTMLImageElement>()
        img.onload = {
            loadedImage = img
            setupCanvasFor(img)
            crop = defaultCrop()
            render()
        }
        img.src = sourceToImgSrc(source)
    }

    private fun sourceToImgSrc(media: Media): String {
        val data = media.data.get().trim()
        if (data.isEmpty()) return ""
        if (
            data.startsWith("data:") ||
            data.startsWith("http://") ||
            data.startsWith("https://") ||
            data.startsWith("blob:")
        ) {
            return data
        }

        val ct = media.contentType.get().trim().ifEmpty {
            field.outputType.trim().ifEmpty { "image/png" }
        }

        return "data:$ct;base64,$data"
    }

    private fun scheduleLivePreview() {
        if (session.closed) return
        if (livePending) return
        livePending = true
        window.requestAnimationFrame {
            livePending = false
            if (session.closed) return@requestAnimationFrame
            val media = cropToMedia() ?: return@requestAnimationFrame
            field.valueProperty.set(media)
        }
    }

    private fun setupCanvasFor(img: HTMLImageElement) {
        val w = max(1, img.naturalWidth)
        val h = max(1, img.naturalHeight)

        val s = min(
            1.0,
            min(field.previewMaxWidth.toDouble() / w.toDouble(), field.previewMaxHeight.toDouble() / h.toDouble())
        )

        previewScale = s
        canvas.width = max(1, (w * s).roundToInt())
        canvas.height = max(1, (h * s).roundToInt())
    }

    private fun defaultCrop(): CropRect {
        val cw = canvas.width.toDouble()
        val ch = canvas.height.toDouble()

        val r = field.aspectRatio
        if (r == null || r <= 0.0) return CropRect(0.0, 0.0, cw, ch)

        var w = cw
        var h = w / r
        if (h > ch) {
            h = ch
            w = h * r
        }
        val x = (cw - w) / 2.0
        val y = (ch - h) / 2.0
        return CropRect(x, y, w, h)
    }

    private fun render() {
        val ctx = canvas.getContext("2d")?.unsafeCast<CanvasRenderingContext2D>() ?: return
        val cw = canvas.width.toDouble()
        val ch = canvas.height.toDouble()

        ctx.clearRect(0.0, 0.0, cw, ch)

        val img = loadedImage ?: return

        ctx.drawImage(img, 0.0, 0.0, cw, ch)

        val r = crop?.normalize() ?: return
        if (r.w <= 0.0 || r.h <= 0.0) return

        // Dim the whole image...
        ctx.fillStyle = "rgba(0,0,0,0.40)"
        ctx.fillRect(0.0, 0.0, cw, ch)

        // ...then redraw the cropped part without dimming.
        ctx.save()
        ctx.beginPath()
        ctx.rect(r.x, r.y, r.w, r.h)
        ctx.clip()
        ctx.drawImage(img, 0.0, 0.0, cw, ch)
        ctx.restore()

        // Crop frame
        ctx.strokeStyle = "rgba(255,255,255,0.92)"
        ctx.lineWidth = 1.0
        ctx.strokeRect(r.x + 0.5, r.y + 0.5, max(0.0, r.w - 1.0), max(0.0, r.h - 1.0))

        // Handles (corners)
        val hs = 6.0
        fun handle(cx: Double, cy: Double) {
            val x = cx - hs / 2
            val y = cy - hs / 2
            ctx.fillStyle = "rgba(255,255,255,0.92)"
            ctx.fillRect(x, y, hs, hs)
            ctx.strokeStyle = "rgba(0,0,0,0.55)"
            ctx.strokeRect(x + 0.5, y + 0.5, hs - 1.0, hs - 1.0)
        }
        handle(r.x, r.y)
        handle(r.x + r.w, r.y)
        handle(r.x, r.y + r.h)
        handle(r.x + r.w, r.y + r.h)
    }

    private fun wireCanvasDragging() {
        val onMouseDown: (Event) -> Unit = onMouseDown@{ e ->
            val me = e.unsafeCast<MouseEvent>()
            if (loadedImage == null) return@onMouseDown

            val p = canvasPoint(me)
            val cur = crop?.normalize()

            val hit = hitTest(cur, p.x, p.y)
            drag = DragState(hit, p.x, p.y, cur)

            if (hit == DragMode.New) {
                crop = CropRect(p.x, p.y, 1.0, 1.0)
            }

            render()
        }

        val onMouseMove: (Event) -> Unit = onMouseMove@{ e ->
            val me = e.unsafeCast<MouseEvent>()
            val st = drag ?: return@onMouseMove
            if (loadedImage == null) return@onMouseMove

            val p = canvasPoint(me)
            val cw = canvas.width.toDouble()
            val ch = canvas.height.toDouble()

            val minSize = 8.0
            val ratio = field.aspectRatio?.takeIf { it > 0.0 }

            fun clampMove(x: Double, y: Double, w: Double, h: Double): CropRect {
                val nx = x.coerceIn(0.0, max(0.0, cw - w))
                val ny = y.coerceIn(0.0, max(0.0, ch - h))
                return CropRect(nx, ny, w, h)
            }

            fun clampRect(r: CropRect): CropRect {
                val rr = r.normalize()
                var x = rr.x
                var y = rr.y
                var w = max(minSize, rr.w)
                var h = max(minSize, rr.h)

                if (w > cw) w = cw
                if (h > ch) h = ch
                if (x < 0.0) x = 0.0
                if (y < 0.0) y = 0.0
                if (x + w > cw) x = cw - w
                if (y + h > ch) y = ch - h
                return CropRect(x, y, w, h)
            }

            when (st.mode) {
                DragMode.Move -> {
                    val r = st.startRect ?: return@onMouseMove
                    val dx = p.x - st.startX
                    val dy = p.y - st.startY
                    crop = clampMove(r.x + dx, r.y + dy, r.w, r.h)
                }
                DragMode.New -> {
                    val dx = p.x - st.startX
                    val dy = p.y - st.startY
                    var r = CropRect(st.startX, st.startY, dx, dy)
                    r = if (ratio == null) {
                        r
                    } else {
                        val nr = r.normalize()
                        val signX = if (dx >= 0) 1 else -1
                        val signY = if (dy >= 0) 1 else -1
                        val w0 = nr.w
                        val h0 = nr.h
                        val (w1, h1) =
                            if (h0 == 0.0) {
                                Pair(w0, if (ratio == 0.0) 0.0 else w0 / ratio)
                            } else if (w0 / h0 > ratio) {
                                Pair(h0 * ratio, h0)
                            } else {
                                Pair(w0, if (ratio == 0.0) 0.0 else w0 / ratio)
                            }
                        CropRect(st.startX, st.startY, w1 * signX, h1 * signY)
                    }
                    crop = clampRect(r)
                }
                DragMode.ResizeNW,
                DragMode.ResizeNE,
                DragMode.ResizeSW,
                DragMode.ResizeSE -> {
                    val r0 = st.startRect ?: return@onMouseMove
                    val anchorX: Double
                    val anchorY: Double
                    val cornerX: Double
                    val cornerY: Double

                    when (st.mode) {
                        DragMode.ResizeNW -> { anchorX = r0.x + r0.w; anchorY = r0.y + r0.h; cornerX = p.x; cornerY = p.y }
                        DragMode.ResizeNE -> { anchorX = r0.x; anchorY = r0.y + r0.h; cornerX = p.x; cornerY = p.y }
                        DragMode.ResizeSW -> { anchorX = r0.x + r0.w; anchorY = r0.y; cornerX = p.x; cornerY = p.y }
                        DragMode.ResizeSE -> { anchorX = r0.x; anchorY = r0.y; cornerX = p.x; cornerY = p.y }
                    }

                    val dx = cornerX - anchorX
                    val dy = cornerY - anchorY

                    var r = CropRect(anchorX, anchorY, dx, dy)

                    r = if (ratio == null) {
                        r
                    } else {
                        val nr = r.normalize()
                        val signX = if (dx >= 0) 1 else -1
                        val signY = if (dy >= 0) 1 else -1
                        val w0 = nr.w
                        val h0 = nr.h
                        val (w1, h1) =
                            if (h0 == 0.0) {
                                Pair(w0, if (ratio == 0.0) 0.0 else w0 / ratio)
                            } else if (w0 / h0 > ratio) {
                                Pair(h0 * ratio, h0)
                            } else {
                                Pair(w0, if (ratio == 0.0) 0.0 else w0 / ratio)
                            }
                        CropRect(anchorX, anchorY, w1 * signX, h1 * signY)
                    }

                    crop = clampRect(r)
                }
            }

            render()
            scheduleLivePreview()
        }

        val onMouseUp: (Event) -> Unit = {
            if (drag != null) {
                drag = null
                render()
            }
        }

        canvas.addEventListener("mousedown", onMouseDown)
        window.addEventListener("mousemove", onMouseMove)
        window.addEventListener("mouseup", onMouseUp)

        onDispose {
            canvas.removeEventListener("mousedown", onMouseDown)
            window.removeEventListener("mousemove", onMouseMove)
            window.removeEventListener("mouseup", onMouseUp)
        }
    }

    private fun hitTest(r: CropRect?, x: Double, y: Double): DragMode {
        if (r == null) return DragMode.New

        val rr = r.normalize()
        val hs = 10.0

        fun near(px: Double, py: Double, cx: Double, cy: Double): Boolean =
            abs(px - cx) <= hs && abs(py - cy) <= hs

        val nw = near(x, y, rr.x, rr.y)
        val ne = near(x, y, rr.x + rr.w, rr.y)
        val sw = near(x, y, rr.x, rr.y + rr.h)
        val se = near(x, y, rr.x + rr.w, rr.y + rr.h)

        return when {
            nw -> DragMode.ResizeNW
            ne -> DragMode.ResizeNE
            sw -> DragMode.ResizeSW
            se -> DragMode.ResizeSE
            x >= rr.x && x <= rr.x + rr.w && y >= rr.y && y <= rr.y + rr.h -> DragMode.Move
            else -> DragMode.New
        }
    }

    private data class Pt(val x: Double, val y: Double)

    private fun canvasPoint(e: MouseEvent): Pt {
        val rect = canvas.getBoundingClientRect()
        val sx = if (rect.width == 0.0) 1.0 else canvas.width.toDouble() / rect.width
        val sy = if (rect.height == 0.0) 1.0 else canvas.height.toDouble() / rect.height

        val x = (e.clientX.toDouble() - rect.left) * sx
        val y = (e.clientY.toDouble() - rect.top) * sy

        return Pt(x, y)
    }

    private fun cropToMedia(): Media? {
        val img = loadedImage ?: return null

        val r = (crop?.normalize() ?: defaultCrop()).normalize()
        if (r.w <= 0.0 || r.h <= 0.0) return null

        val sx = r.x / previewScale
        val sy = r.y / previewScale
        val sw = r.w / previewScale
        val sh = r.h / previewScale

        var outW = max(1, sw.roundToInt())
        var outH = max(1, sh.roundToInt())

        val maxW = field.outputMaxWidth
        val maxH = field.outputMaxHeight
        if ((maxW != null && outW > maxW) || (maxH != null && outH > maxH)) {
            val s = min(
                if (maxW == null) 1.0 else maxW.toDouble() / outW.toDouble(),
                if (maxH == null) 1.0 else maxH.toDouble() / outH.toDouble()
            )
            outW = max(1, (outW * s).roundToInt())
            outH = max(1, (outH * s).roundToInt())
        }

        outCanvas.width = outW
        outCanvas.height = outH

        val octx = outCanvas.getContext("2d")?.unsafeCast<CanvasRenderingContext2D>() ?: return null
        octx.drawImage(img, sx, sy, sw, sh, 0.0, 0.0, outW.toDouble(), outH.toDouble())

        val dataUrl = outCanvas.toDataURL(field.outputType, field.outputQuality)
        val data = base64FromDataUrl(dataUrl) ?: dataUrl

        // Store the cropped image in Thumbnail (used for preview + persistence).
        val thumbData = data

        val name = source.name.get()
        val id = source.id?.get()
        val contentType = field.outputType

        val sourceContentType = source.contentType.get().ifBlank { contentType }
        val sourceData = source.data.get().let { base64FromDataUrl(it) ?: it }

        return Media(
            id = Property(id?: Uuid.generateV4().toString()),
            name = Property(name),
            contentType = Property(sourceContentType),
            data = Property(sourceData),
            thumbnail = Thumbnail(
                id = Property(source.thumbnail.id?.get() ?: Uuid.generateV4().toString()),
                name = Property(source.thumbnail.name.get().ifBlank { name }),
                contentType = Property(contentType),
                data = Property(thumbData),
            )
        )
    }

    private fun base64FromDataUrl(dataUrl: String): String? {
        if (!dataUrl.startsWith("data:")) return null
        val comma = dataUrl.indexOf(',', startIndex = 5)
        if (comma < 0) return null
        return dataUrl.substring(comma + 1)
    }

    companion object {
        private data class CropRect(
            val x: Double,
            val y: Double,
            val w: Double,
            val h: Double,
        ) {
            fun normalize(): CropRect {
                val nx = if (w >= 0) x else x + w
                val ny = if (h >= 0) y else y + h
                return CropRect(nx, ny, abs(w), abs(h))
            }
        }

        private data class DragState(
            val mode: DragMode,
            val startX: Double,
            val startY: Double,
            val startRect: CropRect?,
        )

        private enum class DragMode { New, Move, ResizeNW, ResizeNE, ResizeSW, ResizeSE }

        class ImageCropperSession(
            var applied: Boolean = false,
            var closed: Boolean = false,
            val initialValue: Media?,
        )
    }
}