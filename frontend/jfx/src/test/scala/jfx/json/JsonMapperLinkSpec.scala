package jfx.json

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal as jsObj
import jfx.core.state.{ListProperty, Property}
import jfx.core.meta.Meta
import jfx.form.ErrorResponse

import java.util.UUID

class JsonMapperLinkSpec extends AnyFlatSpec with Matchers {

  "JsonMapper" should "deserialize Link objects" in {
    TestLink.meta
    val jsonMapper = new JsonMapper()

    val linksJson = jsObj(
      "rel" -> "self",
      "url" -> "/api/test",
      "method" -> "GET",
      "id" -> "123"
    )

    val result = jsonMapper.deserialize[TestLink](linksJson, TestLink.meta)

    result.rel.get shouldBe "self"
    result.url.get shouldBe "/api/test"
    result.method.get shouldBe "GET"
    result.id.get shouldBe "123"
  }

  it should "deserialize and serialize TestEntity with links" in {
    TestEntityWithLinks.meta
    TestLink.meta
    val jsonMapper = new JsonMapper()

    val entity = new TestEntityWithLinks()
    val link1 = new TestLink()
    link1.rel.set("self")
    link1.url.set("/api/1")
    link1.method.set("GET")
    link1.id.set("1")
    val link2 = new TestLink()
    link2.rel.set("edit")
    link2.url.set("/api/2")
    link2.method.set("PUT")
    link2.id.set("2")
    entity.links.setAll(Seq(link1, link2))

    val json = jsonMapper.serialize(entity)
    val deserialized = jsonMapper.deserialize[TestEntityWithLinks](json, TestEntityWithLinks.meta)

    deserialized.name.get shouldBe ""
    deserialized.links.length shouldBe 2
    deserialized.links.get(0).rel.get shouldBe "self"
    deserialized.links.get(0).url.get shouldBe "/api/1"
    deserialized.links.get(1).rel.get shouldBe "edit"
    deserialized.links.get(1).url.get shouldBe "/api/2"
  }

  it should "deserialize Property[UUID] from JSON" in {
    TestEntityWithUuid.meta
    val jsonMapper = new JsonMapper()

    val uuid = UUID.randomUUID()
    val json = jsObj(
      "id" -> uuid.toString
    )

    val deserialized = jsonMapper.deserialize[TestEntityWithUuid](json, TestEntityWithUuid.meta)

    deserialized.id.get shouldBe uuid
  }

  it should "serialize and deserialize Property[UUID]" in {
    TestEntityWithUuid.meta
    val jsonMapper = new JsonMapper()

    val entity = new TestEntityWithUuid()
    val uuid = UUID.randomUUID()
    entity.id.set(uuid)

    val json = jsonMapper.serialize(entity)
    val deserialized = jsonMapper.deserialize[TestEntityWithUuid](json, TestEntityWithUuid.meta)

    deserialized.id.get shouldBe uuid
  }

  it should "deserialize an array of ErrorResponse" in {
    TestEntityWithUuid.meta
    val jsonMapper = new JsonMapper()

    val errorsJson = js.Array(
      jsObj("message" -> "Error 1", "path" -> js.Array("field1")),
      jsObj("message" -> "Error 2", "path" -> js.Array("field2", 0))
    )

    val errors = jsonMapper.deserializeArray[TestEntityWithUuid](errorsJson.asInstanceOf[scala.scalajs.js.Dynamic], TestEntityWithUuid.meta)

    errors.length shouldBe 0
  }

}

class TestLink extends jfx.form.Model[TestLink] {
  val rel: Property[String] = Property("")
  val url: Property[String] = Property("")
  val method: Property[String] = Property("")
  val id: Property[String] = Property("")

  override def meta: Meta[TestLink] = TestLink.meta
}

object TestLink {
  val meta: Meta[TestLink] = Meta(() => new TestLink())
}

class TestEntityWithLinks extends jfx.form.Model[TestEntityWithLinks] {
  val name: Property[String] = Property("")
  val links: ListProperty[TestLink] = ListProperty()

  override def meta: Meta[TestEntityWithLinks] = TestEntityWithLinks.meta
}

object TestEntityWithLinks {
  val meta: Meta[TestEntityWithLinks] = Meta(() => new TestEntityWithLinks())
}

class TestEntityWithUuid extends jfx.form.Model[TestEntityWithUuid] {
  val id: Property[UUID] = Property(null.asInstanceOf[UUID])

  override def meta: Meta[TestEntityWithUuid] = TestEntityWithUuid.meta
}

object TestEntityWithUuid {
  val meta: Meta[TestEntityWithUuid] = Meta(() => new TestEntityWithUuid())
}
