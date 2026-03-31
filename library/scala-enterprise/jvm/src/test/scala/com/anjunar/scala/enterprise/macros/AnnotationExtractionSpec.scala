package jfx.json

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import jfx.core.meta.Meta
import com.anjunar.scala.enterprise.macros.validation.{NotBlank, Size}

class AnnotationExtractionSpec extends AnyFlatSpec with Matchers {

  "Annotation extraction" should "extract NotBlank and Size annotations from val fields" in {
    val properties = PropertyMacros.describeProperties[TestModel]
    
    val firstNameProp = properties.find(_.name == "firstName").getOrElse(fail("firstName property not found"))
    
    firstNameProp.annotations should not be empty
    firstNameProp.annotations.length shouldBe 2
    
    val notBlankAnnotation = firstNameProp.annotations.find(_.annotationClassName.endsWith("NotBlank"))
      .getOrElse(fail("NotBlank annotation not found"))
    
    notBlankAnnotation.parameters should contain key "message"
    notBlankAnnotation.parameters("message") shouldBe "Vorname ist erforderlich"
    
    val sizeAnnotation = firstNameProp.annotations.find(_.annotationClassName.endsWith("Size"))
      .getOrElse(fail("Size annotation not found"))
    
    sizeAnnotation.parameters should contain key "min"
    sizeAnnotation.parameters should contain key "max"
    sizeAnnotation.parameters("min") shouldBe 2
    sizeAnnotation.parameters("max") shouldBe 80
    sizeAnnotation.parameters("message") shouldBe "Vorname muss zwischen 2 und 80 Zeichen haben"
  }
}

class TestModel {
  @NotBlank(message = "Vorname ist erforderlich")
  @Size(min = 2, max = 80, message = "Vorname muss zwischen 2 und 80 Zeichen haben")
  val firstName: String = ""
}
