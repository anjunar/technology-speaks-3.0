package com.anjunar.json.mapper.macros

import org.scalatest.funsuite.AnyFunSuite
import jakarta.json.bind.annotation.JsonbProperty
import scala.annotation.meta.field

class PropertyMacrosSpec extends AnyFunSuite {

  trait Links {
    def links: List[String] = List("a", "b")
  }

  case class Simple(name: String, var age: Int)

  class Complex(val user: String) extends Links {
    @JsonbProperty("custom_email")
    var email: String = ""
    
    private var secret: String = "shh"
    
    def greet: String = s"Hello $user"

    def action(): Unit = ()
  }

  test("describeProperties should find fields and parameterless methods") {
    val props = PropertyMacros.describeProperties[Complex]
    val names = props.map(_.name).toSet
    
    assert(names.contains("user"), "Should find constructor val")
    assert(names.contains("email"), "Should find var")
    assert(names.contains("links"), "Should find parameterless method from trait")
    assert(names.contains("greet"), "Should find parameterless method from class")
    
    assert(!names.contains("secret"), "Should not find private members")
    assert(!names.contains("action"), "Should not find methods with empty parameter list ()")
    assert(!names.contains("hashCode"), "Should filter out standard methods")
  }

  test("describeProperties should correctly identify isWriteable") {
    val props = PropertyMacros.describeProperties[Complex]
    
    val userProp = props.find(_.name == "user").get
    assert(!userProp.isWriteable, "val user should not be writeable")
    
    val emailProp = props.find(_.name == "email").get
    assert(emailProp.isWriteable, "var email should be writeable")
    
    val linksProp = props.find(_.name == "links").get
    assert(!linksProp.isWriteable, "def links should not be writeable")
  }
  
  test("describeProperties should find properties in case classes") {
    val props = PropertyMacros.describeProperties[Simple]
    val names = props.map(_.name).toSet
    
    assert(names.contains("name"))
    assert(names.contains("age"))
  }

  test("makePropertyAccess should create accessors") {
    val access = PropertyMacros.makePropertyAccess[Simple, String](_.name)
    val s = Simple("John", 30)
    
    assert(access.name == "name")
    assert(access.get(s) == "John")
    
    val ageAccess = PropertyMacros.makePropertyAccess[Simple, Int](_.age)
    ageAccess.set(s, 31)
    assert(s.age == 31)
  }

  test("describeProperties should capture annotations") {
    val props = PropertyMacros.describeProperties[Complex]
    val emailProp = props.find(_.name == "email").get
    
    val hasAnnotation = emailProp.annotations.exists(_.isInstanceOf[JsonbProperty])
    assert(hasAnnotation, "Should find JsonbProperty annotation")
    
    val ann = emailProp.annotations.find(_.isInstanceOf[JsonbProperty]).get.asInstanceOf[JsonbProperty]
    assert(ann.value() == "custom_email")
  }
}
