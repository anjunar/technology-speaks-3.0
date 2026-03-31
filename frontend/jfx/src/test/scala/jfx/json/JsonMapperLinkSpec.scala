package jfx.json

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{literal => jsObj}
import jfx.core.state.{ListProperty, Property}
import com.anjunar.scala.enterprise.macros.PropertyAccess
import com.anjunar.scala.enterprise.macros.PropertyMacros.makePropertyAccess
import java.util.UUID

class JsonMapperLinkSpec extends AnyFlatSpec with Matchers {

  "JsonMapper" should "deserialize Link objects from $links field" in {
    val registry = new TestJsonRegistry()
    registry.classes.update("Link", () => new TestLink())
    registry.classes.update("TestEntityWithLinks", () => new TestEntityWithLinks())
    val jsonMapper = new JsonMapper(registry)

    val linksJson = jsObj(
      "@type" -> "TestEntityWithLinks",
      "$links" -> js.Array[js.Any](
        jsObj("rel" -> "self", "url" -> "/api/1", "method" -> "GET", "id" -> "1"),
        jsObj("rel" -> "edit", "url" -> "/api/2", "method" -> "PUT", "id" -> "2")
      )
    )

    val deserialized = jsonMapper.deserialize[TestEntityWithLinks](linksJson)

    deserialized.links.length shouldBe 2
    deserialized.links.get(0).rel shouldBe "self"
    deserialized.links.get(0).url shouldBe "/api/1"
    deserialized.links.get(1).rel shouldBe "edit"
    deserialized.links.get(1).url shouldBe "/api/2"
  }

  it should "deserialize single Link object" in {
    val registry = new TestJsonRegistry()
    registry.classes.update("Link", () => new TestLink())
    val jsonMapper = new JsonMapper(registry)

    val linkJson = jsObj(
      "@type" -> "Link",
      "rel" -> "self",
      "url" -> "/api/test",
      "method" -> "GET",
      "id" -> "123"
    )

    val link = jsonMapper.deserialize[TestLink](linkJson)

    link.rel shouldBe "self"
    link.url shouldBe "/api/test"
    link.method shouldBe "GET"
    link.id shouldBe "123"
  }

  it should "serialize and deserialize TestEntity with links" in {
    val registry = new TestJsonRegistry()
    registry.classes.update("Link", () => new TestLink())
    registry.classes.update("TestEntityWithLinks", () => new TestEntityWithLinks())
    val jsonMapper = new JsonMapper(registry)

    val entity = new TestEntityWithLinks()
    val link1 = new TestLink("self", "/api/1", "GET", "1")
    val link2 = new TestLink("edit", "/api/2", "PUT", "2")
    entity.links.setAll(Seq(link1, link2))

    val json = jsonMapper.serialize(entity)
    val deserialized = jsonMapper.deserialize[TestEntityWithLinks](json)

    deserialized.links.length shouldBe 2
    deserialized.links.get(0).rel shouldBe "self"
    deserialized.links.get(0).url shouldBe "/api/1"
    deserialized.links.get(1).rel shouldBe "edit"
    deserialized.links.get(1).url shouldBe "/api/2"
  }

  it should "deserialize Property[UUID] from JSON" in {
    val registry = new TestJsonRegistry()
    val entity = new TestEntityWithUuid()
    registry.classes.update(entity.getClass.getSimpleName, () => new TestEntityWithUuid())
    val jsonMapper = new JsonMapper(registry)

    val uuid = UUID.randomUUID()
    val json = jsObj(
      "@type" -> entity.getClass.getSimpleName,
      "id" -> uuid.toString
    )

    val deserialized = jsonMapper.deserialize[TestEntityWithUuid](json)

    deserialized.id.get shouldBe uuid
  }

  it should "serialize and deserialize Property[UUID]" in {
    val registry = new TestJsonRegistry()
    val entity = new TestEntityWithUuid()
    registry.classes.update(entity.getClass.getSimpleName, () => new TestEntityWithUuid())
    val jsonMapper = new JsonMapper(registry)

    val uuid = UUID.randomUUID()
    entity.id.set(uuid)

    val json = jsonMapper.serialize(entity)
    val deserialized = jsonMapper.deserialize[TestEntityWithUuid](json)

    deserialized.id.get shouldBe uuid
  }
}

class TestJsonRegistry extends JsonRegistry {
  val classes: js.Map[String, () => Any] = js.Map()

  valueFactories += classOf[UUID].getName -> (() => UUID.randomUUID())
  valueDeserializers += classOf[UUID].getName -> ((raw: js.Any) => UUID.fromString(raw.toString))
  valueSerializers += classOf[UUID] -> ((value: Any) => value.asInstanceOf[UUID].toString.asInstanceOf[js.Any])
}

class TestLink(
  var rel: String = "",
  var url: String = "",
  var method: String = "GET",
  var id: String = ""
) extends jfx.form.Model[TestLink] {

  override def properties: Seq[PropertyAccess[TestLink, ?]] = TestLink.properties
}

object TestLink {
  val properties: Seq[PropertyAccess[TestLink, ?]] = Seq(
    makePropertyAccess[TestLink, String](_.rel),
    makePropertyAccess[TestLink, String](_.url),
    makePropertyAccess[TestLink, String](_.method),
    makePropertyAccess[TestLink, String](_.id)
  )
}

class TestEntityWithLinks extends jfx.form.Model[TestEntityWithLinks] {
  val links: ListProperty[TestLink] = ListProperty()

  override def properties: Seq[PropertyAccess[TestEntityWithLinks, ?]] =
    TestEntityWithLinks.properties
}

object TestEntityWithLinks {
  val properties: Seq[PropertyAccess[TestEntityWithLinks, ?]] = Seq(
    makePropertyAccess[TestEntityWithLinks, ListProperty[TestLink]](_.links)
  )
}

class TestEntityWithUuid extends jfx.form.Model[TestEntityWithUuid] {
  val id: Property[UUID] = Property(null.asInstanceOf[UUID])

  override def properties: Seq[PropertyAccess[TestEntityWithUuid, ?]] =
    TestEntityWithUuid.properties
}

object TestEntityWithUuid {
  val properties: Seq[PropertyAccess[TestEntityWithUuid, ?]] = Seq(
    makePropertyAccess[TestEntityWithUuid, Property[UUID]](_.id)
  )
}
