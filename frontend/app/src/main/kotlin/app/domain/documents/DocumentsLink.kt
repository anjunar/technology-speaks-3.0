package app.domain.documents

import app.domain.core.AbstractLink
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("document-root")
class DocumentsLink(
    override val id: String,
    override val rel: String,
    override val url: String,
    override val method : String = "GET") : AbstractLink() {

    override val name : String = "Dokument"
    override val icon: String = "edit_document"
}