package jfx.json

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import jfx.core.state.{ListProperty, Property}
import jfx.core.meta.Meta

import java.util.UUID

class JsonMapperSpec extends AnyFlatSpec with Matchers {

  "JsonMapper" should "deserialize simple object with String properties" in {
    TestPerson.meta
    val jsonMapper = new JsonMapper()
    val person = new TestPerson()
    person.name.set("Test User")
    person.email.set("test@example.com")
    val json = jsonMapper.serialize(person)

    val result = jsonMapper.deserialize[TestPerson](json, TestPerson.meta)

    result.name.get shouldBe "Test User"
    result.email.get shouldBe "test@example.com"
  }

  it should "deserialize numeric values" in {
    TestNumbers.meta
    val jsonMapper = new JsonMapper()
    val numbers = new TestNumbers()
    numbers.age.set(30)
    numbers.salary.set(50000.50)
    val json = jsonMapper.serialize(numbers)

    val result = jsonMapper.deserialize[TestNumbers](json, TestNumbers.meta)

    result.age.get shouldBe 30
    result.salary.get shouldBe 50000.50
  }

  it should "deserialize boolean values" in {
    TestFlags.meta
    val jsonMapper = new JsonMapper()
    val flags = new TestFlags()
    flags.active.set(true)
    flags.verified.set(false)
    val json = jsonMapper.serialize(flags)

    val result = jsonMapper.deserialize[TestFlags](json, TestFlags.meta)

    result.active.get shouldBe true
    result.verified.get shouldBe false
  }

  it should "deserialize UUID" in {
    TestWithUuid.meta
    val jsonMapper = new JsonMapper()
    val uuid = UUID.randomUUID()
    val withUuid = new TestWithUuid()
    withUuid.id.set(uuid)
    val json = jsonMapper.serialize(withUuid)

    val result = jsonMapper.deserialize[TestWithUuid](json, TestWithUuid.meta)

    result.id.get shouldBe uuid
  }

  it should "deserialize Property[String]" in {
    TestWithProperty.meta
    val jsonMapper = new JsonMapper()
    val withProperty = new TestWithProperty()
    withProperty.name.set("PropertyValue")
    val json = jsonMapper.serialize(withProperty)

    val result = jsonMapper.deserialize[TestWithProperty](json, TestWithProperty.meta)

    result.name.get shouldBe "PropertyValue"
  }

  it should "deserialize Property[UUID]" in {
    TestWithPropertyUuid.meta
    val jsonMapper = new JsonMapper()
    val uuid = UUID.randomUUID()
    val withPropertyUuid = new TestWithPropertyUuid()
    withPropertyUuid.id.set(uuid)
    val json = jsonMapper.serialize(withPropertyUuid)

    val result = jsonMapper.deserialize[TestWithPropertyUuid](json, TestWithPropertyUuid.meta)

    result.id.get shouldBe uuid
  }

  it should "deserialize ListProperty" in {
    TestWithListProperty.meta
    val jsonMapper = new JsonMapper()
    val withList = new TestWithListProperty()
    withList.items.setAll(Seq("item1", "item2", "item3"))
    val json = jsonMapper.serialize(withList)

    val result = jsonMapper.deserialize[TestWithListProperty](json, TestWithListProperty.meta)

    result.items.length shouldBe 3
    result.items.get(0) shouldBe "item1"
    result.items.get(1) shouldBe "item2"
    result.items.get(2) shouldBe "item3"
  }

  it should "deserialize Model with links" in {
    TestModelWithLinks.meta
    TestLinkFromSpec.meta
    val jsonMapper = new JsonMapper()
    val entity = new TestModelWithLinks()
    entity.name.set("Entity")
    val link1 = new TestLinkFromSpec()
    link1.rel.set("self")
    link1.url.set("/api/1")
    link1.method.set("GET")
    val link2 = new TestLinkFromSpec()
    link2.rel.set("edit")
    link2.url.set("/api/2")
    link2.method.set("PUT")
    entity.links.setAll(Seq(link1, link2))
    val json = jsonMapper.serialize(entity)

    val result = jsonMapper.deserialize[TestModelWithLinks](json, TestModelWithLinks.meta)

    result.name.get shouldBe "Entity"
    result.links.length shouldBe 2
    result.links.get(0).rel.get shouldBe "self"
    result.links.get(0).url.get shouldBe "/api/1"
    result.links.get(1).rel.get shouldBe "edit"
    result.links.get(1).url.get shouldBe "/api/2"
  }

  it should "serialize and deserialize round-trip" in {
    TestPerson.meta
    val jsonMapper = new JsonMapper()
    val original = new TestPerson()
    original.name.set("Round Trip")
    original.email.set("roundtrip@test.com")

    val json = jsonMapper.serialize(original)
    val deserialized = jsonMapper.deserialize[TestPerson](json, TestPerson.meta)

    deserialized.name.get shouldBe "Round Trip"
    deserialized.email.get shouldBe "roundtrip@test.com"
  }

  it should "serialize and deserialize Model with ListProperty[TestLinkFromSpec] round-trip" in {
    TestModelWithLinks.meta
    TestLinkFromSpec.meta
    val jsonMapper = new JsonMapper()
    val entity = new TestModelWithLinks()
    entity.name.set("Linked Entity")
    val link1 = new TestLinkFromSpec()
    link1.rel.set("self")
    link1.url.set("/api/self")
    link1.method.set("GET")
    val link2 = new TestLinkFromSpec()
    link2.rel.set("delete")
    link2.url.set("/api/delete")
    link2.method.set("DELETE")
    entity.links.setAll(Seq(link1, link2))

    val json = jsonMapper.serialize(entity)
    val deserialized = jsonMapper.deserialize[TestModelWithLinks](json, TestModelWithLinks.meta)

    deserialized.name.get shouldBe "Linked Entity"
    deserialized.links.length shouldBe 2
    deserialized.links.get(0).rel.get shouldBe "self"
    deserialized.links.get(0).url.get shouldBe "/api/self"
    deserialized.links.get(1).rel.get shouldBe "delete"
    deserialized.links.get(1).url.get shouldBe "/api/delete"
  }

  it should "handle null values gracefully" in {
    TestPerson.meta
    val jsonMapper = new JsonMapper()
    val person = new TestPerson()
    person.name.set(null)
    person.email.set("notnull@example.com")
    val json = jsonMapper.serialize(person)

    val result = jsonMapper.deserialize[TestPerson](json, TestPerson.meta)

    result.name.get shouldBe null
    result.email.get shouldBe "notnull@example.com"
  }

  it should "handle missing optional fields" in {
    TestPerson.meta
    val jsonMapper = new JsonMapper()
    val person = new TestPerson()
    person.name.set("OnlyName")
    val json = jsonMapper.serialize(person)

    val result = jsonMapper.deserialize[TestPerson](json, TestPerson.meta)

    result.name.get shouldBe "OnlyName"
  }
}

class TestPerson extends jfx.form.Model[TestPerson] {
  val name: Property[String] = Property("")
  val email: Property[String] = Property("")

  override def meta: Meta[TestPerson] = TestPerson.meta
}

object TestPerson {
  val meta: Meta[TestPerson] = Meta(() => new TestPerson())
}

class TestNumbers extends jfx.form.Model[TestNumbers] {
  val age: Property[Int] = Property(0)
  val salary: Property[Double] = Property(0.0)

  override def meta: Meta[TestNumbers] = TestNumbers.meta
}

object TestNumbers {
  val meta: Meta[TestNumbers] = Meta(() => new TestNumbers())
}

class TestFlags extends jfx.form.Model[TestFlags] {
  val active: Property[Boolean] = Property(false)
  val verified: Property[Boolean] = Property(false)

  override def meta: Meta[TestFlags] = TestFlags.meta
}

object TestFlags {
  val meta: Meta[TestFlags] = Meta(() => new TestFlags())
}

class TestWithUuid extends jfx.form.Model[TestWithUuid] {
  val id: Property[UUID] = Property(null.asInstanceOf[UUID])

  override def meta: Meta[TestWithUuid] = TestWithUuid.meta
}

object TestWithUuid {
  val meta: Meta[TestWithUuid] = Meta(() => new TestWithUuid())
}

class TestWithProperty extends jfx.form.Model[TestWithProperty] {
  val name: Property[String] = Property("")

  override def meta: Meta[TestWithProperty] = TestWithProperty.meta
}

object TestWithProperty {
  val meta: Meta[TestWithProperty] = Meta(() => new TestWithProperty())
}

class TestWithPropertyUuid extends jfx.form.Model[TestWithPropertyUuid] {
  val id: Property[UUID] = Property(null.asInstanceOf[UUID])

  override def meta: Meta[TestWithPropertyUuid] = TestWithPropertyUuid.meta
}

object TestWithPropertyUuid {
  val meta: Meta[TestWithPropertyUuid] = Meta(() => new TestWithPropertyUuid())
}

class TestWithListProperty extends jfx.form.Model[TestWithListProperty] {
  val items: ListProperty[String] = ListProperty()

  override def meta: Meta[TestWithListProperty] = TestWithListProperty.meta
}

object TestWithListProperty {
  val meta: Meta[TestWithListProperty] = Meta(() => new TestWithListProperty())
}

class TestModelWithLinks extends jfx.form.Model[TestModelWithLinks] {
  val name: Property[String] = Property("")
  val links: ListProperty[TestLinkFromSpec] = ListProperty()

  override def meta: Meta[TestModelWithLinks] = TestModelWithLinks.meta
}

object TestModelWithLinks {
  val meta: Meta[TestModelWithLinks] = Meta(() => new TestModelWithLinks())
}

class TestLinkFromSpec extends jfx.form.Model[TestLinkFromSpec] {
  val rel: Property[String] = Property("")
  val url: Property[String] = Property("")
  val method: Property[String] = Property("")

  override def meta: Meta[TestLinkFromSpec] = TestLinkFromSpec.meta
}

object TestLinkFromSpec {
  val meta: Meta[TestLinkFromSpec] = Meta(() => new TestLinkFromSpec())
}
