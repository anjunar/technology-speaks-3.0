package jfx.json

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import reflect.macros.ReflectMacros.reflectType

import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal as jsObj
import jfx.core.state.{ListProperty, Property}
import jfx.json.{JsonIgnore, JsonName}

import java.util.UUID

class JsonMapperSpec extends AnyFlatSpec with Matchers {

  "JsonMapper" should "serialize and deserialize simple model with String properties" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson())
    val jsonMapper = new JsonMapper()
    val person = new TestPerson()
    person.name.set("Test User")
    person.email.set("test@example.com")
    
    val json = jsonMapper.serialize(person)
    val result = jsonMapper.deserialize[TestPerson](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson()))

    result.name.get shouldBe "Test User"
    result.email.get shouldBe "test@example.com"
  }

  it should "include @type field when @JsonType annotation is present" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPersonWithJsonType], () => new TestPersonWithJsonType())
    val jsonMapper = new JsonMapper()
    val person = new TestPersonWithJsonType()
    person.name.set("Typed Person")
    
    val json = jsonMapper.serialize(person)
    
    json.asInstanceOf[js.Dynamic].selectDynamic("@type").toString shouldBe "TestPersonWithJsonType"
  }

  it should "deserialize using @type field when present" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPersonWithJsonType], () => new TestPersonWithJsonType())
    val jsonMapper = new JsonMapper()
    val person = new TestPersonWithJsonType()
    person.name.set("Typed Person")
    
    val json = jsonMapper.serialize(person)
    val result = jsonMapper.deserialize[TestPersonWithJsonType](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPersonWithJsonType], () => new TestPersonWithJsonType()))

    result.name.get shouldBe "Typed Person"
  }

  it should "deserialize numeric values" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestNumbers], () => new TestNumbers())
    val jsonMapper = new JsonMapper()
    val numbers = new TestNumbers()
    numbers.age.set(30)
    numbers.salary.set(50000.50)
    
    val json = jsonMapper.serialize(numbers)
    val result = jsonMapper.deserialize[TestNumbers](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestNumbers], () => new TestNumbers()))

    result.age.get shouldBe 30
    result.salary.get shouldBe 50000.50
  }

  it should "deserialize boolean values" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestFlags], () => new TestFlags())
    val jsonMapper = new JsonMapper()
    val flags = new TestFlags()
    flags.active.set(true)
    flags.verified.set(false)
    
    val json = jsonMapper.serialize(flags)
    val result = jsonMapper.deserialize[TestFlags](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestFlags], () => new TestFlags()))

    result.active.get shouldBe true
    result.verified.get shouldBe false
  }

  it should "deserialize UUID" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithUuid], () => new TestWithUuid())
    val jsonMapper = new JsonMapper()
    val uuid = UUID.randomUUID()
    val withUuid = new TestWithUuid()
    withUuid.id.set(uuid)
    
    val json = jsonMapper.serialize(withUuid)
    val result = jsonMapper.deserialize[TestWithUuid](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithUuid], () => new TestWithUuid()))

    result.id.get shouldBe uuid
  }

  it should "respect @JsonName annotation for field renaming" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithJsonName], () => new TestWithJsonName())
    val jsonMapper = new JsonMapper()
    val model = new TestWithJsonName()
    model.customField.set("Custom Value")
    
    val json = jsonMapper.serialize(model)
    
    val jsonObj = json.asInstanceOf[js.Dynamic]
    js.isUndefined(jsonObj.selectDynamic("customField")) shouldBe true
    js.isUndefined(jsonObj.selectDynamic("renamedField")) shouldBe false
    jsonObj.selectDynamic("renamedField").toString shouldBe "Custom Value"
    
    val result = jsonMapper.deserialize[TestWithJsonName](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithJsonName], () => new TestWithJsonName()))
    result.customField.get shouldBe "Custom Value"
  }

  it should "ignore fields with @JsonIgnore annotation" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithJsonIgnore], () => new TestWithJsonIgnore())
    val jsonMapper = new JsonMapper()
    val model = new TestWithJsonIgnore()
    model.visible.set("Visible")
    model.hidden.set("Hidden")
    
    val json = jsonMapper.serialize(model)
    
    val jsonObj = json.asInstanceOf[js.Dynamic]
    js.isUndefined(jsonObj.selectDynamic("visible")) shouldBe false
    js.isUndefined(jsonObj.selectDynamic("hidden")) shouldBe true
    
    val result = jsonMapper.deserialize[TestWithJsonIgnore](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithJsonIgnore], () => new TestWithJsonIgnore()))
    result.visible.get shouldBe "Visible"
  }

  it should "deserialize ListProperty" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithListProperty], () => new TestWithListProperty())
    val jsonMapper = new JsonMapper()
    val withList = new TestWithListProperty()
    withList.items.setAll(Seq("item1", "item2", "item3"))
    
    val json = jsonMapper.serialize(withList)
    val result = jsonMapper.deserialize[TestWithListProperty](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithListProperty], () => new TestWithListProperty()))

    result.items.length shouldBe 3
    result.items.get(0) shouldBe "item1"
    result.items.get(1) shouldBe "item2"
    result.items.get(2) shouldBe "item3"
  }

  it should "deserialize nested models" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestNestedParent], () => new TestNestedParent())
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestNestedChild], () => new TestNestedChild())
    val jsonMapper = new JsonMapper()
    
    val parent = new TestNestedParent()
    parent.name.set("Parent")
    val child = new TestNestedChild()
    child.value.set("Child Value")
    parent.child.set(child)
    
    val json = jsonMapper.serialize(parent)
    val result = jsonMapper.deserialize[TestNestedParent](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestNestedParent], () => new TestNestedParent()))

    result.name.get shouldBe "Parent"
    result.child.get.value.get shouldBe "Child Value"
  }

  it should "handle null values gracefully" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson())
    val jsonMapper = new JsonMapper()
    val person = new TestPerson()
    person.name.set(null)
    person.email.set("notnull@example.com")
    
    val json = jsonMapper.serialize(person)
    val result = jsonMapper.deserialize[TestPerson](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson()))

    result.name.get shouldBe null
    result.email.get shouldBe "notnull@example.com"
  }

  it should "handle missing optional fields" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson())
    val jsonMapper = new JsonMapper()
    val person = new TestPerson()
    person.name.set("OnlyName")
    
    val json = jsonMapper.serialize(person)
    val result = jsonMapper.deserialize[TestPerson](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson()))

    result.name.get shouldBe "OnlyName"
  }

  it should "throw exception for unknown @type" in {
    val jsonMapper = new JsonMapper()
    
    val json = jsObj(
      "@type" -> "UnknownType",
      "name" -> "Test"
    )
    
    val ex = intercept[IllegalArgumentException] {
      jsonMapper.deserialize[TestPerson](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson()))
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
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson())
    val jsonMapper = new JsonMapper()
    val original = new TestPerson()
    original.name.set("Round Trip")
    original.email.set("roundtrip@test.com")

    val json = jsonMapper.serialize(original)
    val deserialized = jsonMapper.deserialize[TestPerson](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson()))

    deserialized.name.get shouldBe "Round Trip"
    deserialized.email.get shouldBe "roundtrip@test.com"
  }

  it should "serialize and deserialize model with nested models round-trip" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestNestedParent], () => new TestNestedParent())
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestNestedChild], () => new TestNestedChild())
    val jsonMapper = new JsonMapper()

    val parent = new TestNestedParent()
    parent.name.set("Parent")
    val child = new TestNestedChild()
    child.value.set("Child Value")
    parent.child.set(child)

    val json = jsonMapper.serialize(parent)
    val deserialized = jsonMapper.deserialize[TestNestedParent](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestNestedParent], () => new TestNestedParent()))

    deserialized.name.get shouldBe "Parent"
    deserialized.child.get.value.get shouldBe "Child Value"
  }

  it should "serialize and deserialize Map property" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithMap], () => new TestWithMap())
    val jsonMapper = new JsonMapper()

    val withMap = new TestWithMap()
    val mapData = Map(
      "key1" -> "value1",
      "key2" -> "value2",
      "key3" -> "value3"
    )
    withMap.entries.set(mapData)

    val json = jsonMapper.serialize(withMap)
    val deserialized = jsonMapper.deserialize[TestWithMap](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithMap], () => new TestWithMap()))

    deserialized.entries.get shouldBe a[Map[?, ?]]
    deserialized.entries.get.asInstanceOf[Map[String, String]]("key1") shouldBe "value1"
    deserialized.entries.get.asInstanceOf[Map[String, String]]("key2") shouldBe "value2"
    deserialized.entries.get.asInstanceOf[Map[String, String]]("key3") shouldBe "value3"
  }

  it should "serialize Map as JSON object with keys" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithMap], () => new TestWithMap())
    val jsonMapper = new JsonMapper()

    val withMap = new TestWithMap()
    withMap.entries.set(Map("a" -> "alpha", "b" -> "beta"))

    val json = jsonMapper.serialize(withMap)

    val jsonObj = json.asInstanceOf[js.Dynamic]
    js.isUndefined(jsonObj.selectDynamic("entries")) shouldBe false
    jsonObj.selectDynamic("entries").selectDynamic("a").toString shouldBe "alpha"
    jsonObj.selectDynamic("entries").selectDynamic("b").toString shouldBe "beta"
  }

  it should "deserialize Map from JSON object" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithMap], () => new TestWithMap())
    val jsonMapper = new JsonMapper()

    val json = jsObj(
      "entries" -> jsObj(
        "first" -> "one",
        "second" -> "two"
      )
    )

    val result = jsonMapper.deserialize[TestWithMap](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithMap], () => new TestWithMap()))

    result.entries.get shouldBe a[Map[?, ?]]
    val map = result.entries.get.asInstanceOf[Map[String, String]]
    map("first") shouldBe "one"
    map("second") shouldBe "two"
  }

  it should "serialize and deserialize Map with nested models" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithNestedMap], () => new TestWithNestedMap())
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestMapItem], () => new TestMapItem())
    val jsonMapper = new JsonMapper()

    val withMap = new TestWithNestedMap()
    val item1 = new TestMapItem()
    item1.name.set("Item 1")
    val item2 = new TestMapItem()
    item2.name.set("Item 2")
    val mapData = Map(
      "item1" -> item1,
      "item2" -> item2
    )
    withMap.items.set(mapData)

    val json = jsonMapper.serialize(withMap)
    val deserialized = jsonMapper.deserialize[TestWithNestedMap](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithNestedMap], () => new TestWithNestedMap()))

    deserialized.items.get shouldBe a[Map[?, ?]]
    val map = deserialized.items.get.asInstanceOf[Map[String, TestMapItem]]
    map("item1").name.get shouldBe "Item 1"
    map("item2").name.get shouldBe "Item 2"
  }
}

class TestPerson extends jfx.form.Model[TestPerson] {
  val name: Property[String] = Property("")
  val email: Property[String] = Property("")


}

object TestPerson {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson())
}

@JsonType("TestPersonWithJsonType")
class TestPersonWithJsonType extends jfx.form.Model[TestPersonWithJsonType] {
  val name: Property[String] = Property("")


}

object TestPersonWithJsonType {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPersonWithJsonType], () => new TestPersonWithJsonType())
}

class TestNumbers extends jfx.form.Model[TestNumbers] {
  val age: Property[Int] = Property(0)
  val salary: Property[Double] = Property(0.0)


}

object TestNumbers {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestNumbers], () => new TestNumbers())
}

class TestFlags extends jfx.form.Model[TestFlags] {
  val active: Property[Boolean] = Property(false)
  val verified: Property[Boolean] = Property(false)


}

object TestFlags {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestFlags], () => new TestFlags())
}

class TestWithUuid extends jfx.form.Model[TestWithUuid] {
  val id: Property[UUID] = Property(null.asInstanceOf[UUID])


}

object TestWithUuid {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithUuid], () => new TestWithUuid())
}

class TestWithJsonName extends jfx.form.Model[TestWithJsonName] {
  @JsonName("renamedField")
  val customField: Property[String] = Property("")


}

object TestWithJsonName {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithJsonName], () => new TestWithJsonName())
}

class TestWithJsonIgnore extends jfx.form.Model[TestWithJsonIgnore] {
  val visible: Property[String] = Property("")

  @JsonIgnore
  val hidden: Property[String] = Property("")


}

object TestWithJsonIgnore {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithJsonIgnore], () => new TestWithJsonIgnore())
}

class TestWithListProperty extends jfx.form.Model[TestWithListProperty] {
  val items: ListProperty[String] = ListProperty()


}

object TestWithListProperty {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithListProperty], () => new TestWithListProperty())
}

class TestNestedChild extends jfx.form.Model[TestNestedChild] {
  val value: Property[String] = Property("")


}

object TestNestedChild {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestNestedChild], () => new TestNestedChild())
}

class TestNestedParent extends jfx.form.Model[TestNestedParent] {
  val name: Property[String] = Property("")
  val child: Property[TestNestedChild | Null] = Property(null)


}

object TestNestedParent {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestNestedParent], () => new TestNestedParent())
}

class TestWithMap extends jfx.form.Model[TestWithMap] {
  val entries: Property[Map[String, String]] = Property(Map.empty)


}

object TestWithMap {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithMap], () => new TestWithMap())
}

class TestMapItem extends jfx.form.Model[TestMapItem] {
  val name: Property[String] = Property("")


}

object TestMapItem {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestMapItem], () => new TestMapItem())
}

class TestWithNestedMap extends jfx.form.Model[TestWithNestedMap] {
  val items: Property[Map[String, TestMapItem]] = Property(Map.empty)


}

object TestWithNestedMap {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithNestedMap], () => new TestWithNestedMap())
}

class TestUnregistered extends jfx.form.Model[TestUnregistered] {
  val value: Property[String] = Property("")


}

object TestUnregistered {

}
