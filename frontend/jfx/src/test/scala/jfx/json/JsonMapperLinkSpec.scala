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

  "JsonMapper" should "deserialize Link objects with @type" in {
    TestLink.meta
    val jsonMapper = new JsonMapper()

    val linksJson = jsObj(
      "@type" -> "TestLink",
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

  it should "deserialize without @type when meta is provided" in {
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

  it should "throw exception for mismatched @type" in {
    TestLink.meta
    val jsonMapper = new JsonMapper()

    val json = jsObj(
      "@type" -> "WrongType",
      "rel" -> "self",
      "url" -> "/api/test"
    )

    val ex = intercept[IllegalArgumentException] {
      jsonMapper.deserialize[TestLink](json, TestLink.meta)
    }

    ex.getMessage should include("WrongType")
  }

}

@JsonType("TestLink")
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

@JsonType("TestEntityWithLinks")
class TestEntityWithLinks extends jfx.form.Model[TestEntityWithLinks] {
  val name: Property[String] = Property("")
  val links: ListProperty[TestLink] = ListProperty()

  override def meta: Meta[TestEntityWithLinks] = TestEntityWithLinks.meta
}

object TestEntityWithLinks {
  val meta: Meta[TestEntityWithLinks] = Meta(() => new TestEntityWithLinks())
}

@JsonType("TestEntityWithUuid")
class TestEntityWithUuid extends jfx.form.Model[TestEntityWithUuid] {
  val id: Property[UUID] = Property(null.asInstanceOf[UUID])

  override def meta: Meta[TestEntityWithUuid] = TestEntityWithUuid.meta
}

object TestEntityWithUuid {
  val meta: Meta[TestEntityWithUuid] = Meta(() => new TestEntityWithUuid())
}
