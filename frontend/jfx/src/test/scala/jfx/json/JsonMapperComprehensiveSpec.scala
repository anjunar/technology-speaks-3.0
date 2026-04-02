package jfx.json

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import reflect.macros.ReflectMacros.reflectType

import scala.scalajs.js
import js.Dynamic.literal as jsObj
import jfx.core.state.{ListProperty, Property}
import jfx.json.{JsonIgnore, JsonName}
import java.util.UUID

class JsonMapperComprehensiveSpec extends AnyFlatSpec with Matchers {

  "JsonMapper" should "deserialize empty array correctly" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson())
    val jsonMapper = new JsonMapper()
    val emptyArray = js.Array[js.Dynamic]()
    
    val result = jsonMapper.deserializeArray[TestPerson](emptyArray, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson()))
    
    result shouldBe empty
    result.length shouldBe 0
  }

  it should "deserialize array with multiple elements" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson())
    val jsonMapper = new JsonMapper()
    
    val json = js.Array[js.Dynamic](
      jsObj("name" -> "Person 1", "email" -> "p1@test.com"),
      jsObj("name" -> "Person 2", "email" -> "p2@test.com"),
      jsObj("name" -> "Person 3", "email" -> "p3@test.com")
    )
    
    val result = jsonMapper.deserializeArray[TestPerson](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson()))
    
    result.length shouldBe 3
    result(0).name.get shouldBe "Person 1"
    result(0).email.get shouldBe "p1@test.com"
    result(1).name.get shouldBe "Person 2"
    result(2).name.get shouldBe "Person 3"
  }

  it should "return empty seq for null array" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson())
    val jsonMapper = new JsonMapper()
    
    val result = jsonMapper.deserializeArray[TestPerson](null, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson()))
    
    result shouldBe empty
  }

  it should "return empty seq for empty js array" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson())
    val jsonMapper = new JsonMapper()
    
    val result = jsonMapper.deserializeArray[TestPerson](js.Array[js.Dynamic](), reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson()))
    
    result shouldBe empty
  }

  it should "handle deeply nested model structures" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestLevel1], () => new TestLevel1())
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestLevel2], () => new TestLevel2())
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestLevel3], () => new TestLevel3())
    val jsonMapper = new JsonMapper()
    
    val level3 = new TestLevel3()
    level3.value.set("Level 3")
    
    val level2 = new TestLevel2()
    level2.name.set("Level 2")
    level2.child.set(level3)
    
    val level1 = new TestLevel1()
    level1.title.set("Level 1")
    level1.child.set(level2)
    
    val json = jsonMapper.serialize(level1)
    val result = jsonMapper.deserialize[TestLevel1](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestLevel1], () => new TestLevel1()))
    
    result.title.get shouldBe "Level 1"
    result.child.get.name.get shouldBe "Level 2"
    result.child.get.child.get.value.get shouldBe "Level 3"
  }

  it should "handle ListProperty with nested models" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestParentWithChildren], () => new TestParentWithChildren())
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestChild], () => new TestChild())
    val jsonMapper = new JsonMapper()
    
    val parent = new TestParentWithChildren()
    val child1 = new TestChild()
    child1.name.set("Child 1")
    val child2 = new TestChild()
    child2.name.set("Child 2")
    parent.children.setAll(Seq(child1, child2))
    
    val json = jsonMapper.serialize(parent)
    val result = jsonMapper.deserialize[TestParentWithChildren](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestParentWithChildren], () => new TestParentWithChildren()))
    
    result.children.length shouldBe 2
    result.children.get(0).name.get shouldBe "Child 1"
    result.children.get(1).name.get shouldBe "Child 2"
  }

  it should "handle mixed null and non-null properties" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson())
    val jsonMapper = new JsonMapper()
    
    val person = new TestPerson()
    person.name.set("Only Name")
    person.email.set(null)
    
    val json = jsonMapper.serialize(person)
    val result = jsonMapper.deserialize[TestPerson](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson()))
    
    result.name.get shouldBe "Only Name"
    result.email.get shouldBe null
  }

  it should "handle integer and double values correctly" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestMixedNumbers], () => new TestMixedNumbers())
    val jsonMapper = new JsonMapper()
    
    val numbers = new TestMixedNumbers()
    numbers.intVal.set(42)
    numbers.doubleVal.set(3.14159)
    numbers.longVal.set(123456789L)
    numbers.floatVal.set(2.718f)
    
    val json = jsonMapper.serialize(numbers)
    val result = jsonMapper.deserialize[TestMixedNumbers](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestMixedNumbers], () => new TestMixedNumbers()))
    
    result.intVal.get shouldBe 42
    result.doubleVal.get shouldBe 3.14159
    result.longVal.get shouldBe 123456789L
    result.floatVal.get shouldBe 2.718f
  }

  it should "handle empty string values" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson())
    val jsonMapper = new JsonMapper()
    
    val person = new TestPerson()
    person.name.set("")
    person.email.set("")
    
    val json = jsonMapper.serialize(person)
    val result = jsonMapper.deserialize[TestPerson](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson()))
    
    result.name.get shouldBe ""
    result.email.get shouldBe ""
  }

  it should "handle special characters in strings" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson())
    val jsonMapper = new JsonMapper()
    
    val person = new TestPerson()
    person.name.set("Name with \"quotes\" and \\backslash")
    person.email.set("email@with-special.chars_and_üñíçödé.com")
    
    val json = jsonMapper.serialize(person)
    val result = jsonMapper.deserialize[TestPerson](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson()))
    
    result.name.get shouldBe "Name with \"quotes\" and \\backslash"
    result.email.get shouldBe "email@with-special.chars_and_üñíçödé.com"
  }

  it should "handle multiple UUIDs in a model" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestMultipleUuids], () => new TestMultipleUuids())
    val jsonMapper = new JsonMapper()
    
    val uuid1 = UUID.randomUUID()
    val uuid2 = UUID.randomUUID()
    val uuid3 = UUID.randomUUID()
    
    val model = new TestMultipleUuids()
    model.id1.set(uuid1)
    model.id2.set(uuid2)
    model.id3.set(uuid3)
    
    val json = jsonMapper.serialize(model)
    val result = jsonMapper.deserialize[TestMultipleUuids](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestMultipleUuids], () => new TestMultipleUuids()))
    
    result.id1.get shouldBe uuid1
    result.id2.get shouldBe uuid2
    result.id3.get shouldBe uuid3
  }

  it should "handle ListProperty with UUIDs" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithUuidList], () => new TestWithUuidList())
    val jsonMapper = new JsonMapper()
    
    val uuid1 = UUID.randomUUID()
    val uuid2 = UUID.randomUUID()
    val uuid3 = UUID.randomUUID()
    
    val model = new TestWithUuidList()
    model.ids.setAll(Seq(uuid1, uuid2, uuid3))
    
    val json = jsonMapper.serialize(model)
    val result = jsonMapper.deserialize[TestWithUuidList](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithUuidList], () => new TestWithUuidList()))
    
    result.ids.length shouldBe 3
    result.ids.get(0) shouldBe uuid1
    result.ids.get(1) shouldBe uuid2
    result.ids.get(2) shouldBe uuid3
  }

  it should "handle empty ListProperty" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithListProperty], () => new TestWithListProperty())
    val jsonMapper = new JsonMapper()
    
    val model = new TestWithListProperty()
    
    val json = jsonMapper.serialize(model)
    val result = jsonMapper.deserialize[TestWithListProperty](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithListProperty], () => new TestWithListProperty()))
    
    result.items.length shouldBe 0
  }

  it should "handle multiple @JsonName annotations in same model" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestMultipleJsonNames], () => new TestMultipleJsonNames())
    val jsonMapper = new JsonMapper()
    
    val model = new TestMultipleJsonNames()
    model.field1.set("Value 1")
    model.field2.set("Value 2")
    model.field3.set("Value 3")
    
    val json = jsonMapper.serialize(model)
    
    val jsonObj = json.asInstanceOf[js.Dynamic]
    js.isUndefined(jsonObj.selectDynamic("json1")) shouldBe false
    js.isUndefined(jsonObj.selectDynamic("json2")) shouldBe false
    js.isUndefined(jsonObj.selectDynamic("json3")) shouldBe false
    js.isUndefined(jsonObj.selectDynamic("field1")) shouldBe true
    js.isUndefined(jsonObj.selectDynamic("field2")) shouldBe true
    js.isUndefined(jsonObj.selectDynamic("field3")) shouldBe true
    
    val result = jsonMapper.deserialize[TestMultipleJsonNames](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestMultipleJsonNames], () => new TestMultipleJsonNames()))
    result.field1.get shouldBe "Value 1"
    result.field2.get shouldBe "Value 2"
    result.field3.get shouldBe "Value 3"
  }

  it should "handle multiple @JsonIgnore annotations in same model" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestMultipleIgnores], () => new TestMultipleIgnores())
    val jsonMapper = new JsonMapper()
    
    val model = new TestMultipleIgnores()
    model.visible1.set("Visible 1")
    model.visible2.set("Visible 2")
    model.hidden1.set("Hidden 1")
    model.hidden2.set("Hidden 2")
    
    val json = jsonMapper.serialize(model)
    
    val jsonObj = json.asInstanceOf[js.Dynamic]
    js.isUndefined(jsonObj.selectDynamic("visible1")) shouldBe false
    js.isUndefined(jsonObj.selectDynamic("visible2")) shouldBe false
    js.isUndefined(jsonObj.selectDynamic("hidden1")) shouldBe true
    js.isUndefined(jsonObj.selectDynamic("hidden2")) shouldBe true
    
    val result = jsonMapper.deserialize[TestMultipleIgnores](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestMultipleIgnores], () => new TestMultipleIgnores()))
    result.visible1.get shouldBe "Visible 1"
    result.visible2.get shouldBe "Visible 2"
  }

  it should "handle complex nested structure with lists and single references" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestComplexRoot], () => new TestComplexRoot())
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestComplexBranch], () => new TestComplexBranch())
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestComplexLeaf], () => new TestComplexLeaf())
    val jsonMapper = new JsonMapper()
    
    val leaf1 = new TestComplexLeaf()
    leaf1.value.set("Leaf 1")
    val leaf2 = new TestComplexLeaf()
    leaf2.value.set("Leaf 2")
    
    val branch = new TestComplexBranch()
    branch.name.set("Branch")
    branch.leaves.setAll(Seq(leaf1, leaf2))
    
    val root = new TestComplexRoot()
    root.title.set("Root")
    root.mainBranch.set(branch)
    
    val json = jsonMapper.serialize(root)
    val result = jsonMapper.deserialize[TestComplexRoot](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestComplexRoot], () => new TestComplexRoot()))
    
    result.title.get shouldBe "Root"
    result.mainBranch.get.name.get shouldBe "Branch"
    result.mainBranch.get.leaves.length shouldBe 2
    result.mainBranch.get.leaves.get(0).value.get shouldBe "Leaf 1"
    result.mainBranch.get.leaves.get(1).value.get shouldBe "Leaf 2"
  }

  it should "preserve Property state after deserialization" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson())
    val jsonMapper = new JsonMapper()
    
    val person = new TestPerson()
    person.name.set("Test")
    person.email.set("test@test.com")
    
    val json = jsonMapper.serialize(person)
    val result = jsonMapper.deserialize[TestPerson](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson()))
    
    result.name.get shouldBe "Test"
    result.email.get shouldBe "test@test.com"
    
    result.name.set("Updated")
    result.name.get shouldBe "Updated"
    
    result.email.set("updated@test.com")
    result.email.get shouldBe "updated@test.com"
  }

  it should "handle deserialization with partial data" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson())
    val jsonMapper = new JsonMapper()
    
    val json = jsObj(
      "name" -> "Only Name"
    )
    
    val result = jsonMapper.deserialize[TestPerson](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson()))
    
    result.name.get shouldBe "Only Name"
  }

  it should "handle serialization with null properties" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson())
    val jsonMapper = new JsonMapper()
    
    val person = new TestPerson()
    person.name.set(null)
    person.email.set(null)
    
    val json = jsonMapper.serialize(person)
    
    val jsonObj = json.asInstanceOf[js.Dynamic]
    js.isUndefined(jsonObj.selectDynamic("name")) shouldBe true
    js.isUndefined(jsonObj.selectDynamic("email")) shouldBe true
  }

  it should "handle very long string values" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson())
    val jsonMapper = new JsonMapper()
    
    val longString = "A" * 10000
    
    val person = new TestPerson()
    person.name.set(longString)
    person.email.set("short@email.com")
    
    val json = jsonMapper.serialize(person)
    val result = jsonMapper.deserialize[TestPerson](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson()))
    
    result.name.get shouldBe longString
    result.name.get.length shouldBe 10000
  }

  it should "handle unicode characters" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson())
    val jsonMapper = new JsonMapper()
    
    val person = new TestPerson()
    person.name.set("你好 世界 Привет мир مرحبا بالعالم")
    person.email.set("unicode@test.com")
    
    val json = jsonMapper.serialize(person)
    val result = jsonMapper.deserialize[TestPerson](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson()))
    
    result.name.get shouldBe "你好 世界 Привет мир مرحبا بالعالم"
  }

  it should "handle model with only ignored fields" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestAllIgnored], () => new TestAllIgnored())
    val jsonMapper = new JsonMapper()
    
    val model = new TestAllIgnored()
    model.hidden1.set("Hidden 1")
    model.hidden2.set("Hidden 2")
    
    val json = jsonMapper.serialize(model)
    
    val jsonObj = json.asInstanceOf[js.Dynamic]
    js.isUndefined(jsonObj.selectDynamic("hidden1")) shouldBe true
    js.isUndefined(jsonObj.selectDynamic("hidden2")) shouldBe true
  }

  it should "deserialize ListProperty correctly when list contains null elements" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithListProperty], () => new TestWithListProperty())
    val jsonMapper = new JsonMapper()
    
    val json = jsObj(
      "items" -> js.Array("item1", null, "item3")
    )
    
    val result = jsonMapper.deserialize[TestWithListProperty](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithListProperty], () => new TestWithListProperty()))
    
    result.items.length shouldBe 3
    result.items.get(0) shouldBe "item1"
    result.items.get(1) shouldBe null
    result.items.get(2) shouldBe "item3"
  }

  it should "handle repeated serialization of same object" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson())
    val jsonMapper = new JsonMapper()
    
    val person = new TestPerson()
    person.name.set("Test")
    person.email.set("test@test.com")
    
    val json1 = jsonMapper.serialize(person)
    val json2 = jsonMapper.serialize(person)
    val json3 = jsonMapper.serialize(person)
    
    json1.toString shouldBe json2.toString
    json2.toString shouldBe json3.toString
  }

  it should "handle repeated deserialization of same JSON" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson())
    val jsonMapper = new JsonMapper()
    
    val json = jsObj(
      "name" -> "Test",
      "email" -> "test@test.com"
    )
    
    val result1 = jsonMapper.deserialize[TestPerson](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson()))
    val result2 = jsonMapper.deserialize[TestPerson](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson()))
    val result3 = jsonMapper.deserialize[TestPerson](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson()))
    
    result1.name.get shouldBe result2.name.get
    result2.email.get shouldBe result3.email.get
    result1.name.get shouldBe "Test"
  }

  it should "handle numeric edge cases" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestEdgeNumbers], () => new TestEdgeNumbers())
    val jsonMapper = new JsonMapper()
    
    val numbers = new TestEdgeNumbers()
    numbers.zero.set(0)
    numbers.negativeInt.set(-42)
    numbers.negativeDouble.set(-3.14)
    numbers.maxInt.set(Int.MaxValue)
    numbers.minInt.set(Int.MinValue)
    
    val json = jsonMapper.serialize(numbers)
    val result = jsonMapper.deserialize[TestEdgeNumbers](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestEdgeNumbers], () => new TestEdgeNumbers()))
    
    result.zero.get shouldBe 0
    result.negativeInt.get shouldBe -42
    result.negativeDouble.get shouldBe -3.14
    result.maxInt.get shouldBe Int.MaxValue
    result.minInt.get shouldBe Int.MinValue
  }

  it should "handle deserializing from JSON with additional unknown fields" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson())
    val jsonMapper = new JsonMapper()
    
    val json = jsObj(
      "name" -> "Test",
      "email" -> "test@test.com",
      "unknownField" -> "Should be ignored",
      "anotherUnknown" -> 12345
    )
    
    val result = jsonMapper.deserialize[TestPerson](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson()))
    
    result.name.get shouldBe "Test"
    result.email.get shouldBe "test@test.com"
  }

  it should "handle JSON with @type but no matching factory falls back to meta type" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson())
    val jsonMapper = new JsonMapper()
    
    val json = jsObj(
      "@type" -> "DifferentButCompatible",
      "name" -> "Fallback Test",
      "email" -> "fallback@test.com"
    )
    
    val result = jsonMapper.deserialize[TestPerson](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestPerson], () => new TestPerson()))
    
    result.name.get shouldBe "Fallback Test"
    result.email.get shouldBe "fallback@test.com"
  }

  it should "handle multiple levels of nested JSON objects" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestNestedParent], () => new TestNestedParent())
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestNestedChild], () => new TestNestedChild())
    val jsonMapper = new JsonMapper()
    
    val json = jsObj(
      "name" -> "Parent",
      "child" -> jsObj(
        "value" -> "Child Value"
      )
    )
    
    val result = jsonMapper.deserialize[TestNestedParent](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestNestedParent], () => new TestNestedParent()))
    
    result.name.get shouldBe "Parent"
    result.child.get.value.get shouldBe "Child Value"
  }

  it should "handle JSON array as ListProperty input" in {
    reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithListProperty], () => new TestWithListProperty())
    val jsonMapper = new JsonMapper()
    
    val json = jsObj(
      "items" -> js.Array("a", "b", "c", "d", "e")
    )
    
    val result = jsonMapper.deserialize[TestWithListProperty](json, reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithListProperty], () => new TestWithListProperty()))
    
    result.items.length shouldBe 5
    result.items.get(0) shouldBe "a"
    result.items.get(4) shouldBe "e"
  }

}

class TestLevel1 extends jfx.form.Model[TestLevel1] {
  val title: Property[String] = Property("")
  val child: Property[TestLevel2 | Null] = Property(null)


}

object TestLevel1 {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestLevel1], () => new TestLevel1())
}

class TestLevel2 extends jfx.form.Model[TestLevel2] {
  val name: Property[String] = Property("")
  val child: Property[TestLevel3 | Null] = Property(null)


}

object TestLevel2 {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestLevel2], () => new TestLevel2())
}

class TestLevel3 extends jfx.form.Model[TestLevel3] {
  val value: Property[String] = Property("")


}

object TestLevel3 {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestLevel3], () => new TestLevel3())
}

class TestParentWithChildren extends jfx.form.Model[TestParentWithChildren] {
  val name: Property[String] = Property("")
  val children: ListProperty[TestChild] = ListProperty()


}

object TestParentWithChildren {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestParentWithChildren], () => new TestParentWithChildren())
}

class TestChild extends jfx.form.Model[TestChild] {
  val name: Property[String] = Property("")


}

object TestChild {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestChild], () => new TestChild())
}

class TestMixedNumbers extends jfx.form.Model[TestMixedNumbers] {
  val intVal: Property[Int] = Property(0)
  val doubleVal: Property[Double] = Property(0.0)
  val longVal: Property[Long] = Property(0L)
  val floatVal: Property[Float] = Property(0f)


}

object TestMixedNumbers {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestMixedNumbers], () => new TestMixedNumbers())
}

class TestMultipleUuids extends jfx.form.Model[TestMultipleUuids] {
  val id1: Property[UUID] = Property(null.asInstanceOf[UUID])
  val id2: Property[UUID] = Property(null.asInstanceOf[UUID])
  val id3: Property[UUID] = Property(null.asInstanceOf[UUID])


}

object TestMultipleUuids {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestMultipleUuids], () => new TestMultipleUuids())
}

class TestWithUuidList extends jfx.form.Model[TestWithUuidList] {
  val ids: ListProperty[UUID] = ListProperty()


}

object TestWithUuidList {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestWithUuidList], () => new TestWithUuidList())
}

class TestMultipleJsonNames extends jfx.form.Model[TestMultipleJsonNames] {
  @JsonName("json1")
  val field1: Property[String] = Property("")
  
  @JsonName("json2")
  val field2: Property[String] = Property("")
  
  @JsonName("json3")
  val field3: Property[String] = Property("")


}

object TestMultipleJsonNames {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestMultipleJsonNames], () => new TestMultipleJsonNames())
}

class TestMultipleIgnores extends jfx.form.Model[TestMultipleIgnores] {
  val visible1: Property[String] = Property("")
  val visible2: Property[String] = Property("")
  
  @JsonIgnore
  val hidden1: Property[String] = Property("")
  
  @JsonIgnore
  val hidden2: Property[String] = Property("")


}

object TestMultipleIgnores {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestMultipleIgnores], () => new TestMultipleIgnores())
}

class TestComplexRoot extends jfx.form.Model[TestComplexRoot] {
  val title: Property[String] = Property("")
  val mainBranch: Property[TestComplexBranch | Null] = Property(null)


}

object TestComplexRoot {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestComplexRoot], () => new TestComplexRoot())
}

class TestComplexBranch extends jfx.form.Model[TestComplexBranch] {
  val name: Property[String] = Property("")
  val leaves: ListProperty[TestComplexLeaf] = ListProperty()


}

object TestComplexBranch {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestComplexBranch], () => new TestComplexBranch())
}

class TestComplexLeaf extends jfx.form.Model[TestComplexLeaf] {
  val value: Property[String] = Property("")


}

object TestComplexLeaf {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestComplexLeaf], () => new TestComplexLeaf())
}

class TestAllIgnored extends jfx.form.Model[TestAllIgnored] {
  @JsonIgnore
  val hidden1: Property[String] = Property("")
  
  @JsonIgnore
  val hidden2: Property[String] = Property("")


}

object TestAllIgnored {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestAllIgnored], () => new TestAllIgnored())
}

class TestEdgeNumbers extends jfx.form.Model[TestEdgeNumbers] {
  val zero: Property[Int] = Property(0)
  val negativeInt: Property[Int] = Property(0)
  val negativeDouble: Property[Double] = Property(0.0)
  val maxInt: Property[Int] = Property(0)
  val minInt: Property[Int] = Property(0)


}

object TestEdgeNumbers {
  reflect.ReflectRegistry.register(reflect.macros.ReflectMacros.reflect[TestEdgeNumbers], () => new TestEdgeNumbers())
}
