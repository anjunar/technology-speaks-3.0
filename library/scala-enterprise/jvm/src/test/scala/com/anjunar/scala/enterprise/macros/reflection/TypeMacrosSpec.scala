package com.anjunar.scala.enterprise.macros.reflection

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.anjunar.scala.enterprise.macros.reflection.TypeMacros

class TypeMacrosSpec extends AnyFlatSpec with Matchers {

  "TypeMacros.toType" should "convert simple class to SimpleClass" in {
    val stringType = TypeMacros.toType[String]
    
    stringType shouldBe a[SimpleClass[?]]
    stringType.getTypeName shouldBe "java.lang.String"
  }

  it should "convert Int to SimpleClass" in {
    val intType = TypeMacros.toType[Int]
    
    intType shouldBe a[SimpleClass[?]]
    intType.getTypeName shouldBe "int"
  }

  it should "convert parameterized type Data[User] to ParameterizedType" in {
    val dataType = TypeMacros.toType[Data[User]]
    
    dataType shouldBe a[ParameterizedType]
    dataType.getTypeName should startWith("com.anjunar.scala.enterprise.macros.reflection.Data[")
    
    val paramType = dataType.asInstanceOf[ParameterizedType]
    paramType.typeArguments.length shouldBe 1
    paramType.typeArguments(0).getTypeName should include("User")
  }

  it should "convert parameterized type List[String] to ParameterizedType" in {
    val listType = TypeMacros.toType[List[String]]
    
    listType shouldBe a[ParameterizedType]
    
    val paramType = listType.asInstanceOf[ParameterizedType]
    paramType.typeArguments.length shouldBe 1
    paramType.typeArguments(0).getTypeName shouldBe "java.lang.String"
  }

  it should "convert Map[String, Int] to ParameterizedType with two type arguments" in {
    val mapType = TypeMacros.toType[Map[String, Int]]
    
    mapType shouldBe a[ParameterizedType]
    
    val paramType = mapType.asInstanceOf[ParameterizedType]
    paramType.typeArguments.length shouldBe 2
    paramType.typeArguments(0).getTypeName shouldBe "java.lang.String"
    paramType.typeArguments(1).getTypeName shouldBe "int"
  }

  "TypeMacros.toSimpleClass" should "convert simple class to SimpleClass" in {
    val stringClass = TypeMacros.toSimpleClass[String]
    
    stringClass shouldBe a[SimpleClass[String]]
    stringClass.getTypeName shouldBe "java.lang.String"
  }

  it should "not accept parameterized types" in {
    "TypeMacros.toSimpleClass[Data[User]]" shouldNot compile
  }
}

case class Data[T](value: T)
case class User(name: String)
