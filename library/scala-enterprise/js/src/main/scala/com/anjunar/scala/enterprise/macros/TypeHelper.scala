package com.anjunar.scala.enterprise.macros

import com.anjunar.scala.enterprise.macros.reflection.{ParameterizedType, SimpleClass, Type}

import scala.annotation.tailrec

object TypeHelper {

  @tailrec
  final def rawType(aType: Type): Class[?] = aType match {
    case aClass: SimpleClass[?] => MetaClassLoader.classes.find(tuple => tuple._1.getName == aClass.typeName).get._1
    case parameterizedType: ParameterizedType => rawType(parameterizedType.rawType)
    case _ => throw new IllegalStateException("Unexpected value: " + aType)
  }

  @tailrec
  final def simpleRawType(aType: Type): SimpleClass[?] = aType match {
    case aClass: SimpleClass[?] => aClass
    case parameterizedType: ParameterizedType => simpleRawType(parameterizedType.rawType)
    case _ => throw new IllegalStateException("Unexpected value: " + aType)
  }

  def entityTypeResolve(propertyType: Type): Class[?] = {
    propertyType match {
      case clazz: SimpleClass[?] => MetaClassLoader.classes.find(tuple => tuple._1.getName == clazz.typeName).get._1
      case parameterizedType: ParameterizedType if classOf[java.util.Collection[?]].isAssignableFrom(rawType(parameterizedType)) =>
        rawType(parameterizedType.typeArguments.head)
      case _ => null
    }
  }


}
