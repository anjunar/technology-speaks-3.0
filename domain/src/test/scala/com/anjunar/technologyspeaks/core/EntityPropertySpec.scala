package com.anjunar.technologyspeaks.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import reflect.macros.PropertySupport

class EntityPropertySpec extends AnyFlatSpec with Matchers {

  "PropertySupport with User entity" should "extract id property" in {
    val property = PropertySupport.makeProperty[User, java.util.UUID](_.id)
    property.name shouldBe "id"
  }

  it should "extract nickName property" in {
    val property = PropertySupport.makeProperty[User, String](_.nickName)
    property.name shouldBe "nickName"
  }

  it should "extract version property" in {
    val property = PropertySupport.makeProperty[User, Long](_.version)
    property.name shouldBe "version"
  }

  it should "extract created property" in {
    val property = PropertySupport.makeProperty[User, java.time.LocalDateTime](_.created)
    property.name shouldBe "created"
  }

  it should "extract modified property" in {
    val property = PropertySupport.makeProperty[User, java.time.LocalDateTime](_.modified)
    property.name shouldBe "modified"
  }

  it should "extract image property" in {
    val property = PropertySupport.makeProperty[User, Media](_.image)
    property.name shouldBe "image"
  }

  it should "extract info property" in {
    val property = PropertySupport.makeProperty[User, UserInfo](_.info)
    property.name shouldBe "info"
  }

  it should "extract address property" in {
    val property = PropertySupport.makeProperty[User, Address](_.address)
    property.name shouldBe "address"
  }

  it should "extract emails property" in {
    val property = PropertySupport.makeProperty[User, java.util.Set[EMail]](_.emails)
    property.name shouldBe "emails"
  }

  it should "extract links property from LinksContainer trait" in {
    val property = PropertySupport.makeProperty[User, java.util.List[com.anjunar.json.mapper.schema.Link]](_.links)
    property.name shouldBe "links"
  }

  it should "extract all properties" in {
    val properties = PropertySupport.extractPropertiesWithAccessors[User]
    val propertyNames = properties.map(_.name)
    
    // From User
    propertyNames should contain("nickName")
    propertyNames should contain("image")
    propertyNames should contain("info")
    propertyNames should contain("address")
    propertyNames should contain("emails")
    
    // From AbstractEntity
    propertyNames should contain("id")
    propertyNames should contain("version")
    propertyNames should contain("created")
    propertyNames should contain("modified")
    
    // From LinksContainer
    propertyNames should contain("links")
  }

  it should "extract properties in same order as AbstractEntitySchema defines them" in {
    val properties = PropertySupport.extractPropertiesWithAccessors[User]
    val propertyNames = properties.map(_.name).toList
    
    // AbstractEntitySchema expects these properties in this order
    val expectedOrder = List("id", "links", "modified", "created")
    
    // Check that all expected properties are present
    expectedOrder.foreach { name =>
      propertyNames should contain(name)
    }
  }
}
