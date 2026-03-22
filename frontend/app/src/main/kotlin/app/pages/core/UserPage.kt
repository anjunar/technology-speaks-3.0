package app.pages.core

import app.domain.core.Address
import app.domain.core.Data
import app.domain.core.User
import app.domain.core.UserInfo
import app.domain.followers.Group
import app.domain.followers.GroupAssignmentRequest
import jFx2.client.ErrorResponseException
import jFx2.client.JsonClient
import jFx2.controls.button
import jFx2.controls.text
import jFx2.core.Component
import jFx2.core.capabilities.NodeScope
import jFx2.core.codegen.JfxComponentBuilder
import jFx2.core.dsl.className
import jFx2.core.dsl.onClick
import jFx2.core.dsl.style
import jFx2.core.dsl.subscribeBidirectional
import jFx2.core.rendering.observeRender
import jFx2.core.template
import jFx2.forms.*
import jFx2.layout.div
import jFx2.layout.hbox
import jFx2.layout.vbox
import jFx2.modals.Viewport
import jFx2.router.PageInfo
import jFx2.router.renderByRel
import jFx2.state.JobRegistry
import jFx2.state.ListProperty
import jFx2.state.Property
import jFx2.table.DataProvider
import jFx2.table.SortState
import org.w3c.dom.HTMLDivElement

@JfxComponentBuilder(classes = ["user-page"])
class UserPage(override val node: HTMLDivElement) : Component<HTMLDivElement>(), PageInfo {
    override val name: String = "User"
    override val width: Int = -1
    override val height: Int = -1
    override val resizable: Boolean = false
    override var close: () -> Unit = {}

    val model = Property(Data(User()))
    val infoDisabled = Property(true)
    val addressDisabled = Property(true)

    private val assignedGroups = ListProperty<Group>()
    private val groupsBusy = Property(false)

    private val groupProvider = object : DataProvider<Group> {
        override val totalCount: Property<Int?> = Property(0)
        override val sortState: Property<SortState?> = Property(null)
        override suspend fun loadRange(offset: Int, limit: Int): List<Group> {
            val table = Group.list(offset, limit)
            totalCount.set(table.size)
            return table.rows.map { it.data }
        }
    }

    fun model(data: Data<User>) {
        model.set(data)
        if (data.data.info.get() != null) infoDisabled.set(false)
        if (data.data.address.get() != null) addressDisabled.set(false)

        val user = data.data
        val link = user.links.find { it.rel == "groups" }

        assignedGroups.clear()

        if (link != null) {
            JobRegistry.instance.launch("loadGroups", owner = this@UserPage) {
                groupsBusy.set(true)
                try {
                    val rows = JsonClient.invoke<List<Data<Group>>>("/service${link.url}")
                    assignedGroups.setAll(rows.map { it.data })
                } finally {
                    groupsBusy.set(false)
                }
            }
        }
    }

    context(scope: NodeScope)
    fun afterBuild() {
        template {
            hbox {
                form(model = model.get().data, clazz = User::class) {

                    disabled = model.links.find { it.rel == "update" } == null

                    onSubmit {

                        try {
                            this@form.model.update()
                            Viewport.notify("Benutzer gespeichert!", Viewport.Companion.NotificationKind.SUCCESS)
                        } catch (e: ErrorResponseException) {
                            Viewport.notify("Fehler im Benutzer", Viewport.Companion.NotificationKind.ERROR)
                            this.setErrors(e.errors)
                        }

                    }

                    vbox {
                        hbox {

                            imageCropper("image") {

                                style {
                                    width = "512px"
                                    height = "512px"
                                }

                                aspectRatio = 1.0
                                outputType = "image/jpeg"
                                outputQuality = 0.92
                                outputMaxWidth = 512
                                outputMaxHeight = 512

                                addValidator(NotBlankValidator())

                                subscribeBidirectional(this@form.model.image, valueProperty)
                            }

                            div {

                                style {
                                    width = "300px"
                                }

                                inputContainer("Nick Name") {
                                    input("nickName") {
                                        addValidator(SizeValidator(2, 80))
                                        subscribeBidirectional(this@form.model.nickName, valueProperty)
                                    }
                                }

                                hbox {

                                    style {
                                        alignItems = "flex-start"
                                    }

                                    observeRender(infoDisabled) { value ->
                                        subForm("info", model = this@form.model.info.get(), clazz = UserInfo::class) {

                                            style {
                                                flex = "1"
                                            }

                                            disabled = value

                                            inputContainer("Vorname") {
                                                input("firstName") {
                                                    addValidator(SizeValidator(2, 80))
                                                    subscribeBidirectional(this@subForm.model.firstName, valueProperty)
                                                }
                                            }

                                            inputContainer("Nachame") {
                                                input("lastName") {
                                                    addValidator(SizeValidator(2, 80))
                                                    subscribeBidirectional(this@subForm.model.lastName, valueProperty)
                                                }
                                            }

                                            inputContainer("Geburtsdatum") {
                                                input("birthDate") {
                                                    type("date")
                                                    addValidator(SizeValidator(2, 80))
                                                    subscribeBidirectional(this@subForm.model.birthDate, valueProperty)
                                                }
                                            }
                                        }
                                    }

                                    renderByRel("update", this@UserPage.model.get().data.links) {
                                        button("close") {
                                            type("button")
                                            className { "material-icons" }
                                            onClick {
                                                if (infoDisabled.get()) {
                                                    this@form.model.info.set(this@form.subForms["info"]!!.model as UserInfo)
                                                    infoDisabled.set(false)
                                                } else {
                                                    this@form.model.info.set(null)
                                                    infoDisabled.set(true)
                                                }
                                            }
                                        }
                                    }

                                }

                                hbox {
                                    style {
                                        alignItems = "flex-start"
                                    }

                                    observeRender(addressDisabled) { value ->
                                        subForm("address", model = this@form.model.address.get(), clazz = Address::class) {

                                            style {
                                                flex = "1"
                                            }

                                            disabled = value

                                            inputContainer("Strasse") {
                                                input("street") {
                                                    addValidator(SizeValidator(2, 80))
                                                    subscribeBidirectional(this@subForm.model.street, valueProperty)
                                                }
                                            }
                                            inputContainer("Hausnummer") {
                                                input("number") {
                                                    addValidator(SizeValidator(1, 10))
                                                    subscribeBidirectional(this@subForm.model.number, valueProperty)
                                                }
                                            }
                                            inputContainer("Postleitzahl") {
                                                input("zipCode") {
                                                    addValidator(SizeValidator(5, 5))
                                                    subscribeBidirectional(this@subForm.model.zipCode, valueProperty)
                                                }
                                            }
                                            inputContainer("Land") {
                                                input("country") {
                                                    addValidator(SizeValidator(2, 80))
                                                    subscribeBidirectional(this@subForm.model.country, valueProperty)
                                                }
                                            }
                                        }
                                    }

                                    renderByRel("update", this@UserPage.model.get().data.links) {
                                        button("close") {
                                            type("button")
                                            className { "material-icons" }
                                            onClick {
                                                if (addressDisabled.get()) {
                                                    this@form.model.address.set(this@form.subForms["address"]!!.model as Address)
                                                    addressDisabled.set(false)
                                                } else {
                                                    this@form.model.address.set(null)
                                                    addressDisabled.set(true)
                                                }
                                            }
                                        }
                                    }
                                }

                            }

                        }

                        hbox {

                            style {
                                justifyContent = "flex-end"
                                columnGap = "10px"
                            }

                            renderByRel("update", model.links) {
                                button("Speichern") {}
                            }

                        }

                    }
                }

                hbox {

                    style {
                        justifyContent = "flex-end"
                        alignItems = "flex-start"
                        padding = "10px"
                    }

                    renderByRel("follow", model.get().data.links) { link ->
                        button("Folgen") {
                            type("button")
                            onClick {
                                JobRegistry.instance.launch("follow", owner = this@UserPage) {
                                    JsonClient.invoke(link, null)
                                }
                            }
                        }
                    }

                    renderByRel("unfollow", model.get().data.links) { link ->

                        vbox {

                            style {
                                alignItems = "flex-end"
                            }

                            button("Nicht mehr folgen") {

                                style {
                                    padding = "0px"
                                }

                                type("button")
                                onClick {
                                    JobRegistry.instance.launch("unfollow", owner = this@UserPage) {
                                        JsonClient.invoke(link, null)
                                    }
                                }
                            }

                            comboBox(name = "groups", provider = groupProvider) {

                                placeholder("Gruppen")

                                subscribeBidirectional(assignedGroups, valueProperty)

                                staticRenderer {
                                    template {
                                        hbox {
                                            val newGroup = input("newGroup") {
                                                placeholder("Neue Gruppe")
                                            }

                                            button("save") {
                                                type("button")
                                                className { "material-icons" }
                                                onClick {
                                                    JobRegistry.instance.launch("addGroup", owner = this@UserPage) {
                                                        val group = Group()
                                                        group.name.set(newGroup.valueProperty.get())
                                                        group.save()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                itemRenderer { group, _ ->
                                    template { div {
                                        text(group.name.get())
                                    } }
                                }

                                valueRenderer { it.name.get() }

                            }

                            renderByRel("updateGroups", model.get().data.links) { updateLink ->
                                button("Gruppen speichern") {
                                    type("button")
                                    onClick {
                                        if (groupsBusy.get()) return@onClick

                                        JobRegistry.instance.launch("updateGroups", owner = this@UserPage) {
                                            groupsBusy.set(true)
                                            try {
                                                val ids = assignedGroups.get().mapNotNull { it.id?.get() }
                                                val updated = JsonClient.invoke<GroupAssignmentRequest, List<Data<Group>>>(
                                                    updateLink,
                                                    GroupAssignmentRequest(ids)
                                                )
                                                assignedGroups.setAll(updated.map { it.data })
                                                Viewport.notify("Gruppen gespeichert!", Viewport.Companion.NotificationKind.SUCCESS)
                                            } finally {
                                                groupsBusy.set(false)
                                            }
                                        }
                                    }
                                }
                            }
                        }


                    }

                }
            }

        }
    }
}
