package com.anjunar.scala.universe.introspector

import com.anjunar.scala.universe.{ResolvedClass, TypeResolver}

import java.lang.reflect.Type
import java.util
import java.util.{HashMap, Map, Objects}

object BeanIntrospector {

  private val cache = new util.HashMap[ResolvedClass, BeanModel]

  def create(aClass: ResolvedClass): BeanModel = {
    var beanModel = cache.get(aClass)
    if (Objects.isNull(beanModel)) {
      beanModel = new BeanModel(aClass)
      cache.put(aClass, beanModel)
    }
    beanModel
  }

  def createWithType(aType: Type): BeanModel = create(TypeResolver.resolve(aType))


}
