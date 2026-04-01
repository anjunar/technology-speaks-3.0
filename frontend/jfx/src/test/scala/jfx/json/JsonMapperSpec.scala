package jfx.json

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal as jsObj
import jfx.core.state.{ListProperty, Property}
import jfx.core.meta.Meta
import com.anjunar.scala.enterprise.macros.validation.JsonName
import jfx.json.JsonIgnore

import java.util.UUID

class JsonMapperSpec extends AnyFlatSpec with Matchers {

  "JsonMapper" should "serialize and deserialize simple model with String properties" in {
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

  it should "include @type field when @JsonType annotation is present" in {
    TestPersonWithJsonType.meta
    val jsonMapper = new JsonMapper()
    val person = new TestPersonWithJsonType()
    person.name.set("Typed Person")
    
    val json = jsonMapper.serialize(person)
    
    json.asInstanceOf[js.Dynamic].selectDynamic("@type").toString shouldBe "TestPersonWithJsonType"
  }

  it should "deserialize using @type field when present" in {
    TestPersonWithJsonType.meta
    val jsonMapper = new JsonMapper()
    val person = new TestPersonWithJsonType()
    person.name.set("Typed Person")
    
    val json = jsonMapper.serialize(person)
    val result = jsonMapper.deserialize[TestPersonWithJsonType](json, TestPersonWithJsonType.meta)

    result.name.get shouldBe "Typed Person"
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

  it should "respect @JsonName annotation for field renaming" in {
    TestWithJsonName.meta
    val jsonMapper = new JsonMapper()
    val model = new TestWithJsonName()
    model.customField.set("Custom Value")
    
    val json = jsonMapper.serialize(model)
    
    val jsonObj = json.asInstanceOf[js.Dynamic]
    js.isUndefined(jsonObj.selectDynamic("customField")) shouldBe true
    js.isUndefined(jsonObj.selectDynamic("renamedField")) shouldBe false
    jsonObj.selectDynamic("renamedField").toString shouldBe "Custom Value"
    
    val result = jsonMapper.deserialize[TestWithJsonName](json, TestWithJsonName.meta)
    result.customField.get shouldBe "Custom Value"
  }

  it should "ignore fields with @JsonIgnore annotation" in {
    TestWithJsonIgnore.meta
    val jsonMapper = new JsonMapper()
    val model = new TestWithJsonIgnore()
    model.visible.set("Visible")
    model.hidden.set("Hidden")
    
    val json = jsonMapper.serialize(model)
    
    val jsonObj = json.asInstanceOf[js.Dynamic]
    js.isUndefined(jsonObj.selectDynamic("visible")) shouldBe false
    js.isUndefined(jsonObj.selectDynamic("hidden")) shouldBe true
    
    val result = jsonMapper.deserialize[TestWithJsonIgnore](json, TestWithJsonIgnore.meta)
    result.visible.get shouldBe "Visible"
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

  it should "deserialize nested models" in {
    TestNestedParent.meta
    TestNestedChild.meta
    val jsonMapper = new JsonMapper()
    
    val parent = new TestNestedParent()
    parent.name.set("Parent")
    val child = new TestNestedChild()
    child.value.set("Child Value")
    parent.child.set(child)
    
    val json = jsonMapper.serialize(parent)
    val result = jsonMapper.deserialize[TestNestedParent](json, TestNestedParent.meta)

    result.name.get shouldBe "Parent"
    result.child.get.value.get shouldBe "Child Value"
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

  it should "throw exception for unknown @type" in {
    val jsonMapper = new JsonMapper()
    
    val json = jsObj(
      "@type" -> "UnknownType",
      "name" -> "Test"
    )
    
    val ex = intercept[IllegalArgumentException] {
      jsonMapper.deserialize[TestPerson](json, TestPerson.meta)
    }
    
    ex.getMessage should include("UnknownType")
    ex.getMessage should include("Available types")
  }

  it should "throw exception when factory not registered" in {
    val jsonMapper = new JsonMapper()
    val model = new TestUnregistered()
    
    val ex = intercept[IllegalArgumentException] {
      jsonMapper.serialize(model)
    }
    
    ex.getMessage should include("TestUnregistered")
    ex.getMessage should include("Meta")
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

  it should "serialize and deserialize model with nested models round-trip" in {
    TestNestedParent.meta
    TestNestedChild.meta
    val jsonMapper = new JsonMapper()
    
    val parent = new TestNestedParent()
    parent.name.set("Parent")
    val child = new TestNestedChild()
    child.value.set("Child Value")
    parent.child.set(child)
    
    val json = jsonMapper.serialize(parent)
    val deserialized = jsonMapper.deserialize[TestNestedParent](json, TestNestedParent.meta)

    deserialized.name.get shouldBe "Parent"
    deserialized.child.get.value.get shouldBe "Child Value"
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

@JsonType("TestPersonWithJsonType")
class TestPersonWithJsonType extends jfx.form.Model[TestPersonWithJsonType] {
  val name: Property[String] = Property("")

  override def meta: Meta[TestPersonWithJsonType] = TestPersonWithJsonType.meta
}

object TestPersonWithJsonType {
  val meta: Meta[TestPersonWithJsonType] = Meta(() => new TestPersonWithJsonType())
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

class TestWithJsonName extends jfx.form.Model[TestWithJsonName] {
  @JsonName("renamedField")
  val customField: Property[String] = Property("")

  override def meta: Meta[TestWithJsonName] = TestWithJsonName.meta
}

object TestWithJsonName {
  val meta: Meta[TestWithJsonName] = Meta(() => new TestWithJsonName())
}

class TestWithJsonIgnore extends jfx.form.Model[TestWithJsonIgnore] {
  val visible: Property[String] = Property("")
  
  @JsonIgnore
  val hidden: Property[String] = Property("")

  override def meta: Meta[TestWithJsonIgnore] = TestWithJsonIgnore.meta
}

object TestWithJsonIgnore {
  val meta: Meta[TestWithJsonIgnore] = Meta(() => new TestWithJsonIgnore())
}

class TestWithListProperty extends jfx.form.Model[TestWithListProperty] {
  val items: ListProperty[String] = ListProperty()

  override def meta: Meta[TestWithListProperty] = TestWithListProperty.meta
}

object TestWithListProperty {
  val meta: Meta[TestWithListProperty] = Meta(() => new TestWithListProperty())
}

class TestNestedChild extends jfx.form.Model[TestNestedChild] {
  val value: Property[String] = Property("")

  override def meta: Meta[TestNestedChild] = TestNestedChild.meta
}

object TestNestedChild {
  val meta: Meta[TestNestedChild] = Meta(() => new TestNestedChild())
}

class TestNestedParent extends jfx.form.Model[TestNestedParent] {
  val name: Property[String] = Property("")
  val child: Property[TestNestedChild | Null] = Property(null)

  override def meta: Meta[TestNestedParent] = TestNestedParent.meta
}

object TestNestedParent {
  val meta: Meta[TestNestedParent] = Meta(() => new TestNestedParent())
}

class TestUnregistered extends jfx.form.Model[TestUnregistered] {
  val value: Property[String] = Property("")

  override def meta: Meta[TestUnregistered] = TestUnregistered.meta
}

object TestUnregistered {
  val meta: Meta[TestUnregistered] = Meta(() => new TestUnregistered())
}
