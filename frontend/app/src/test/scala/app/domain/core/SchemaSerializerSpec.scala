package app.domain.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import reflect.ReflectRegistry

import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal as jsObj
import jfx.json.JsonMapper
import reflect.macros.ReflectMacros.reflectType

class SchemaSerializerSpec extends AnyFlatSpec with Matchers {

  "Schema" should "serialize with properties directly in object, not in entries field" in {
    ReflectRegistry.register(() => new Schema())
    ReflectRegistry.register(() => new SchemaProperty())
    reflectType[Schema]
    reflectType[SchemaProperty]
    
    val jsonMapper = new JsonMapper()
    val schema = new Schema()
    
    val nickNameProp = new SchemaProperty()
    nickNameProp.name = "nickName"
    nickNameProp.`type` = "String"
    
    val imageProp = new SchemaProperty()
    imageProp.name = "image"
    imageProp.`type` = "Media"
    
    schema.entries = Map(
      "nickName" -> nickNameProp,
      "image" -> imageProp
    )
    
    val json = jsonMapper.serialize(schema)
    
    val jsonObj = json.asInstanceOf[js.Dynamic]
    jsonObj.selectDynamic("@type").toString shouldBe "Schema"
    
    json.asInstanceOf[js.Dynamic].selectDynamic("nickName") should not be js.undefined
    json.asInstanceOf[js.Dynamic].selectDynamic("image") should not be js.undefined
    
    val nickNameJson = jsonObj.selectDynamic("nickName").asInstanceOf[js.Dynamic]
    nickNameJson.selectDynamic("@type").toString shouldBe "Property"
    nickNameJson.selectDynamic("name").toString shouldBe "nickName"
    nickNameJson.selectDynamic("type").toString shouldBe "String"
  }

  it should "deserialize from properties directly in object" in {
    ReflectRegistry.register(() => new Schema())
    ReflectRegistry.register(() => new SchemaProperty())
    reflectType[Schema]
    reflectType[SchemaProperty]
    
    val jsonMapper = new JsonMapper()
    
    val json = jsObj(
      "@type" -> "Schema",
      "nickName" -> jsObj(
        "@type" -> "Property",
        "name" -> "nickName",
        "type" -> "String"
      ),
      "image" -> jsObj(
        "@type" -> "Property",
        "name" -> "image",
        "type" -> "Media"
      )
    )
    
    val schema = jsonMapper.deserialize[Schema](json, reflectType[Schema])
    
    schema.entries should not be null
    schema.entries.size shouldBe 2
    
    schema.entries.get("nickName") should not be null
    schema.entries.get("nickName").get.name shouldBe "nickName"
    schema.entries.get("nickName").get.`type` shouldBe "String"
    
    schema.entries.get("image") should not be null
    schema.entries.get("image").get.name shouldBe "image"
    schema.entries.get("image").get.`type` shouldBe "Media"
  }

  it should "serialize and deserialize nested schema" in {
    ReflectRegistry.register(() => new Schema())
    ReflectRegistry.register(() => new SchemaProperty())
    reflectType[Schema]
    reflectType[SchemaProperty]
    
    val jsonMapper = new JsonMapper()
    
    val innerSchema = new Schema()
    val nameProp = new SchemaProperty()
    nameProp.name = "name"
    nameProp.`type` = "String"
    innerSchema.entries = Map("name" -> nameProp)
    
    val imageProp = new SchemaProperty()
    imageProp.name = "image"
    imageProp.`type` = "Media"
    imageProp.schema = innerSchema
    
    val schema = new Schema()
    schema.entries = Map("image" -> imageProp)
    
    val json = jsonMapper.serialize(schema)
    
    val deserialized = jsonMapper.deserialize[Schema](json, reflectType[Schema])
    
    val imageEntry = deserialized.entries.get("image").get
    imageEntry.name shouldBe "image"
    imageEntry.schema should not be null
    
    val deserializedInner = imageEntry.schema.asInstanceOf[Schema]
    deserializedInner.entries.get("name") should not be null
  }

  it should "deserialize schema from backend JSON" in {
    ReflectRegistry.register(() => new Schema())
    ReflectRegistry.register(() => new SchemaProperty())
    reflectType[Schema]
    reflectType[SchemaProperty]
    
    val jsonMapper = new JsonMapper()
    
    // Backend Schema JSON wie im Chat gezeigt
    val json = jsObj(
      "@type" -> "Schema",
      "nickName" -> jsObj(
        "@type" -> "Property",
        "type" -> "String"
      ),
      "image" -> jsObj(
        "@type" -> "Property",
        "type" -> "Media",
        "schema" -> jsObj(
          "@type" -> "Schema",
          "name" -> jsObj(
            "@type" -> "Property",
            "type" -> "String"
          ),
          "contentType" -> jsObj(
            "@type" -> "Property",
            "type" -> "String"
          ),
          "data" -> jsObj(
            "@type" -> "Property",
            "type" -> "Array"
          ),
          "thumbnail" -> jsObj(
            "@type" -> "Property",
            "type" -> "Thumbnail",
            "schema" -> jsObj(
              "@type" -> "Schema",
              "id" -> jsObj(
                "@type" -> "Property",
                "type" -> "UUID"
              ),
              "name" -> jsObj(
                "@type" -> "Property",
                "type" -> "String"
              )
            )
          )
        )
      ),
      "info" -> jsObj(
        "@type" -> "Property",
        "type" -> "UserInfo",
        "schema" -> jsObj(
          "@type" -> "Schema",
          "firstName" -> jsObj(
            "@type" -> "Property",
            "type" -> "String"
          ),
          "lastName" -> jsObj(
            "@type" -> "Property",
            "type" -> "String"
          ),
          "birthDate" -> jsObj(
            "@type" -> "Property",
            "type" -> "LocalDate"
          )
        )
      ),
      "address" -> jsObj(
        "@type" -> "Property",
        "type" -> "Address",
        "schema" -> jsObj(
          "@type" -> "Schema",
          "street" -> jsObj(
            "@type" -> "Property",
            "type" -> "String"
          ),
          "number" -> jsObj(
            "@type" -> "Property",
            "type" -> "String"
          ),
          "zipCode" -> jsObj(
            "@type" -> "Property",
            "type" -> "String"
          ),
          "country" -> jsObj(
            "@type" -> "Property",
            "type" -> "String"
          )
        )
      ),
      "emails" -> jsObj(
        "@type" -> "Property",
        "type" -> "Set"
      )
    )
    
    val schema = jsonMapper.deserialize[Schema](json, reflectType[Schema])
    
    schema.entries should not be null
    schema.entries.size shouldBe 5  // emails (Set) wird nicht als Map behandelt
    
    schema.entries.get("nickName") should not be null
    schema.entries.get("nickName").get.`type` shouldBe "String"
    
    schema.entries.get("image") should not be null
    schema.entries.get("image").get.`type` shouldBe "Media"
    schema.entries.get("image").get.schema should not be null
    
    val imageSchema = schema.entries.get("image").get.schema.asInstanceOf[Schema]
    imageSchema.entries.get("name") should not be null
    imageSchema.entries.get("name").get.`type` shouldBe "String"
    imageSchema.entries.get("thumbnail") should not be null
    
    val thumbnailProp = imageSchema.entries.get("thumbnail").get
    thumbnailProp.schema should not be null
    val thumbnailSchema = thumbnailProp.schema.asInstanceOf[Schema]
    thumbnailSchema.entries.get("id") should not be null
    thumbnailSchema.entries.get("name") should not be null
  }

}
