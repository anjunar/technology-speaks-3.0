package app.domain.timeline

import app.domain.DomainRegistry
import app.domain.core.{Data, Table}
import jfx.json.JsonMapper
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import reflect.ClassDescriptor
import reflect.macros.ReflectMacros.reflectType

import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal as jsObj

class PostDeserializationSpec extends AnyFlatSpec with Matchers {

  DomainRegistry.init()

  private def postJson =
    {
      val post = jsObj(
        "@type" -> "Post",
        "id" -> "ac1c5bb9-6874-4320-8d25-7fc34dab817c",
        "editor" -> jsObj("@type" -> "Node"),
        "likes" -> js.Array()
      )
      post.updateDynamic("$links")(
        js.Array(
          jsObj(
            "@type" -> "Link",
            "rel" -> "like",
            "url" -> "/timeline/posts/post/ac1c5bb9-6874-4320-8d25-7fc34dab817c/like",
            "method" -> "POST"
          ),
          jsObj(
            "@type" -> "Link",
            "rel" -> "send-to-curation",
            "url" -> "/timeline/posts/post/ac1c5bb9-6874-4320-8d25-7fc34dab817c/curation-candidates",
            "method" -> "POST"
          )
        )
      )
      post
    }

  "Post descriptor" should "keep JsonName annotation for links" in {
    val directDescriptor = reflectType[Post].asInstanceOf[ClassDescriptor]
    val registeredDescriptor = ClassDescriptor.forName("app.domain.timeline.Post")
    val directLinksProperty = directDescriptor.getProperty("links").orNull
    val registeredLinksProperty = registeredDescriptor.getProperty("links").orNull

    directLinksProperty should not be null
    registeredLinksProperty should not be null
    withClue(s"direct annotations=${directLinksProperty.annotations.map(_.annotationClassName).mkString(",")}") {
      directLinksProperty.annotations.exists(_.annotationClassName == "jfx.json.JsonName") shouldBe true
    }
    withClue(s"registered annotations=${registeredLinksProperty.annotations.map(_.annotationClassName).mkString(",")}") {
      registeredLinksProperty.annotations.exists(_.annotationClassName == "jfx.json.JsonName") shouldBe true
    }
  }

  "Post deserialization" should "keep hateoas links on direct post payload" in {
    val post = JsonMapper.deserialize[Post](postJson, reflectType[Post])

    post.links.size shouldBe 2
    post.links.get(0).rel shouldBe "like"
    post.links.get(1).rel shouldBe "send-to-curation"
  }

  "Post list deserialization" should "keep hateoas links on posts inside table rows" in {
    val json = jsObj(
      "@type" -> "Table",
      "rows" -> js.Array(
        jsObj(
          "@type" -> "Data",
          "data" -> postJson
        )
      ),
      "size" -> 1
    )

    val table = JsonMapper.deserialize[Table[Data[Post]]](json, reflectType[Table[Data[Post]]])
    val post = table.rows(0).data

    post.links.size shouldBe 2
    post.links.get(0).rel shouldBe "like"
    post.links.get(1).rel shouldBe "send-to-curation"
  }
}
