package jFx2.forms

import app.domain.core.Media
import app.domain.core.Thumbnail
import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.modals.Viewport
import jFx2.state.Disposable
import jFx2.state.ListChange
import jFx2.state.ListProperty
import jFx2.state.Property
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.files.File
import org.w3c.files.FileReader

@JfxComponentBuilder(classes = ["image-cropper-field"])
class ImageCropper(override val node: HTMLDivElement, override val name: String) : FormField<Media?, HTMLDivElement>(), HasPlaceholder {

    // Cropped (or original) result as Media (base64 in Media.data).
    val valueProperty = Property<Media?>(null)

    // Source image used for editing (original upload).
    val sourceProperty = Property<Media?>(null)

    val fileProperty = Property<File?>(null)

    val validatorsProperty = ListProperty<Validator>()

    val editable = Property(true)

    // If set, crop rectangle keeps this width/height ratio.
    var aspectRatio: Double? = null

    // Used by the dialog to size the crop canvas preview.
    var previewMaxWidth: Int = 480
    var previewMaxHeight: Int = 360

    var outputType: String = "image/png"
    var outputQuality: Double = 0.92

    // Optional output size cap; when set, the cropped image is downscaled to fit.
    var outputMaxWidth: Int? = null
    var outputMaxHeight: Int? = null

    
    var thumbnailMaxWidth: Int = 160
    var thumbnailMaxHeight: Int = 160

    var windowTitle: String = "Crop Image"

    private var defaultData: String? = null

    private lateinit var fileInput: HTMLInputElement
    private lateinit var previewImg: HTMLImageElement
    private lateinit var cropBtn: HTMLButtonElement
    private lateinit var clearBtn: HTMLButtonElement

    override var placeholder: String = ""

    override var disabled: Boolean
        get() = ! editable.get()
        set(value) {
            editable.set(!value)
        }

    fun addValidator(validator: Validator) {
        validatorsProperty.add(validator)
    }

    override fun observeValue(listener: (Media?) -> Unit): Disposable = valueProperty.observe(listener)

    override fun read(): Media? = valueProperty.get()

    context(scope: NodeScope)
    fun afterBuild() {
        defaultData = valueProperty.get()?.data?.get()

        node.classList.add("image-cropper")
        // Make the field focusable so it can emit "focus" status for InputContainer.
        node.tabIndex = 0

        val toolbar = scope.create<HTMLDivElement>("div").also { it.classList.add("toolbar") }

        fileInput = scope.create<HTMLInputElement>("input").also {
            it.type = "file"
            it.accept = "image/*"
        }

        cropBtn = scope.create<HTMLButtonElement>("button").also {
            it.type = "button"
            it.textContent = "Crop"
            it.classList.add("hover")
        }

        clearBtn = scope.create<HTMLButtonElement>("button").also {
            it.type = "button"
            it.textContent = "Clear"
            it.classList.add("hover")
        }

        onDispose(editable.observe { editable ->
            if (editable) {
                toolbar.appendChild(fileInput)
                toolbar.appendChild(cropBtn)
                toolbar.appendChild(clearBtn)
            } else {
                toolbar.removeChild(fileInput)
                toolbar.removeChild(cropBtn)
                toolbar.removeChild(clearBtn)
            }
        })

        previewImg = scope.create<HTMLImageElement>("img").also { it.classList.add("preview") }

        node.appendChild(toolbar)
        node.appendChild(previewImg)

        onDispose(bindStatusClasses(node, statusProperty))

        val onFileChange: (Event) -> Unit = onFileChange@{
            val f = fileInput.files?.item(0)
            fileProperty.set(f)
            if (f == null) return@onFileChange

            val reader = FileReader()
            reader.onload = {
                val src = reader.result as? String
                if (src != null) {
                    val media = mediaFromFile(f, src)
                    sourceProperty.set(media)
                    // Update the outside preview immediately; cropping happens in a window.
                    valueProperty.set(media)
                    openCropWindow(media)
                }
                scope.ui.build.flush()
            }
            reader.readAsDataURL(f)
        }
        fileInput.addEventListener("change", onFileChange)
        onDispose { fileInput.removeEventListener("change", onFileChange) }

        val onCropClick: (Event) -> Unit = onCropClick@{
            val src = sourceProperty.get() ?: valueProperty.get()
            val data = src?.data?.get()
            if (src == null || data.isNullOrBlank()) return@onCropClick
            openCropWindow(src)
            scope.ui.build.flush()
        }
        cropBtn.addEventListener("click", onCropClick)
        onDispose { cropBtn.removeEventListener("click", onCropClick) }

        val onClearClick: (Event) -> Unit = {
            clear()
            scope.ui.build.flush()
        }
        clearBtn.addEventListener("click", onClearClick)
        onDispose { clearBtn.removeEventListener("click", onClearClick) }

        // Keep preview in sync even if external bindings set valueProperty directly.
        onDispose(valueProperty.observe { v ->
            val preview = previewSrc(v)
            if (preview.isNullOrBlank()) {
                previewImg.removeAttribute("src")
            } else {
                previewImg.src = preview
            }

            val currentData = v?.data?.get().orEmpty()
            if (currentData.isBlank()) statusProperty.add(Status.empty.name) else statusProperty.remove(Status.empty.name)
            if (currentData != defaultData.orEmpty()) statusProperty.add(Status.dirty.name) else statusProperty.remove(Status.dirty.name)

            validate()
        })

        // If the user sets value but not source, use it as the editable source image.
        onDispose(valueProperty.observe { v ->
            if (v != null && sourceProperty.get() == null) {
                sourceProperty.set(v)
            }
        })

        // Focus handling for InputContainer styles.
        val onFocusIn: (Event) -> Unit = { statusProperty.add(Status.focus.name); scope.ui.build.flush() }
        val onFocusOut: (Event) -> Unit = { statusProperty.remove(Status.focus.name); scope.ui.build.flush() }
        node.addEventListener("focusin", onFocusIn)
        node.addEventListener("focusout", onFocusOut)
        onDispose {
            node.removeEventListener("focusin", onFocusIn)
            node.removeEventListener("focusout", onFocusOut)
        }
    }

    private fun openCropWindow(source: Media) {
        if (source.data.get().isBlank()) return

        val session = ImageCropperDialog.Companion.ImageCropperSession(initialValue = valueProperty.get())

        lateinit var conf: Viewport.Companion.WindowConf
        conf = Viewport.Companion.WindowConf(
            title = windowTitle,
            component = {
                imageCropperDialog(this@ImageCropper, conf, source, session)
            },
            onClose = {
                session.closed = true
                if (!session.applied) {
                    valueProperty.set(session.initialValue)
                }
            }
        )

        Viewport.addWindow(conf)
    }

    private fun mediaFromFile(file: File, dataUrl: String): Media {
        val name = file.name
        val contentType = file.type.takeIf { it.isNotBlank() }
            ?: mimeTypeFromDataUrl(dataUrl).orEmpty()
        val base64 = base64FromDataUrl(dataUrl) ?: dataUrl

        return Media(
            name = Property(name),
            contentType = Property(contentType),
            data = Property(base64),
            thumbnail = Thumbnail(
                name = Property(name),
                contentType = Property(contentType),
                data = Property(""),
            )
        )
    }

    private fun previewSrc(media: Media?): String? {
        if (media == null) return null

        val thumbData = media.thumbnail.data.get().takeIf { it.isNotBlank() }
        if (thumbData != null) {
            val ct = media.thumbnail.contentType.get().ifBlank { media.contentType.get() }
            return toDataUrl(ct, thumbData)
        }

        val data = media.data.get()
        if (data.isBlank()) return null
        return toDataUrl(media.contentType.get(), data)
    }

    private fun toDataUrl(contentType: String, dataOrUrl: String): String? {
        val data = dataOrUrl.trim()
        if (data.isEmpty()) return null
        if (
            data.startsWith("data:") ||
            data.startsWith("http://") ||
            data.startsWith("https://") ||
            data.startsWith("blob:")
        ) {
            return data
        }
        val ct = contentType.trim()
        if (ct.isEmpty()) return null
        return "data:$ct;base64,$data"
    }

    private fun mimeTypeFromDataUrl(dataUrl: String): String? {
        if (!dataUrl.startsWith("data:")) return null
        val semi = dataUrl.indexOf(';', startIndex = 5)
        val comma = dataUrl.indexOf(',', startIndex = 5)
        val end = listOf(semi, comma).filter { it > 5 }.minOrNull() ?: return null
        return dataUrl.substring(5, end)
    }

    private fun base64FromDataUrl(dataUrl: String): String? {
        if (!dataUrl.startsWith("data:")) return null
        val comma = dataUrl.indexOf(',', startIndex = 5)
        if (comma < 0) return null
        return dataUrl.substring(comma + 1)
    }

    private fun validate() {
        val current = valueProperty.get()?.data?.get().orEmpty()
        val errors = validatorsProperty.get().filter { !it.validate(current) }
        if (errors.isNotEmpty()) {
            statusProperty.add(Status.invalid.name)
            statusProperty.remove(Status.valid.name)
        } else {
            statusProperty.remove(Status.invalid.name)
            statusProperty.add(Status.valid.name)
        }
        errorsProperty.setAll(errors.map { it.message() })
    }

    private fun clear() {
        fileInput.value = ""
        fileProperty.set(null)
        sourceProperty.set(null)
        valueProperty.set(null)
    }

    private fun bindStatusClasses(node: Element, status: ListProperty<String>): Disposable {
        val owned = LinkedHashSet<String>()

        fun add(cls: String) {
            val c = cls.trim()
            if (c.isEmpty()) return
            if (owned.add(c)) node.classList.add(c)
        }

        fun remove(cls: String) {
            val c = cls.trim()
            if (c.isEmpty()) return
            if (owned.remove(c)) node.classList.remove(c)
        }

        fun resync(items: List<String>) {
            for (c in owned) node.classList.remove(c)
            owned.clear()
            for (c in items) add(c)
        }

        resync(status.get())

        return status.observeChanges { change ->
            when (change) {
                is ListChange.Add -> change.items.forEach(::add)
                is ListChange.Remove -> change.items.forEach(::remove)
                is ListChange.Replace -> { change.old.forEach(::remove); change.new.forEach(::add) }
                is ListChange.Clear -> { for (c in owned) node.classList.remove(c); owned.clear() }
                is ListChange.SetAll -> resync(change.new)
            }
        }
    }
}