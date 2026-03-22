package com.anjunar.scala.universe.introspector

import com.anjunar.scala.universe.ResolvedClass
import com.anjunar.scala.universe.annotations.Annotated
import com.anjunar.scala.universe.members.{ResolvedField, ResolvedMethod}

import java.lang.annotation.Annotation
import java.util
import java.util.regex.{Matcher, Pattern}

class BeanModel(val underlying: ResolvedClass) extends Annotated {

  private val getterRegex = Pattern.compile("(?:is|get)(\\w+)")

  lazy val declaredProperties: Array[BeanProperty] = {
    underlying.declaredMethods
      .filter(method => {
        val matcher: Matcher = getterRegex.matcher(method.name)
        matcher.matches && method.parameters.length == 0
      })
      .map(getterMethod => {
        val matcher = getterRegex.matcher(getterMethod.name)
        if (matcher.matches) {
          val group = matcher.group(1)
          val propertyName = group.substring(0, 1).toLowerCase + group.substring(1)
          val field = underlying.declaredFields.find((field: ResolvedField) => field.name == propertyName).orNull
          val setterMethod = underlying.declaredMethods.find((method: ResolvedMethod) => method.name == ("set" + group) && method.parameters.length == 1).orNull
          new BeanProperty(this, propertyName, field, getterMethod, setterMethod)
        } else {
          throw new IllegalStateException("no getter found " + getterMethod.name)
        }
      })
  }

  lazy val properties: Array[BeanProperty] = {
    underlying.methods
      .filter(method => {
        val matcher: Matcher = getterRegex.matcher(method.name)
        matcher.matches && method.parameters.length == 0
      })
      .map(getterMethod => {
        val matcher = getterRegex.matcher(getterMethod.name)
        if (matcher.matches) {
          val group = matcher.group(1)
          val propertyName = group.substring(0, 1).toLowerCase + group.substring(1)
          val field = underlying.fields.find((field: ResolvedField) => field.name == propertyName).orNull
          val setterMethod = underlying.methods.find((method: ResolvedMethod) => method.name == ("set" + group) && method.parameters.length == 1).orNull
          new BeanProperty(this, propertyName, field, getterMethod, setterMethod)
        } else {
          throw new IllegalStateException("no getter found " + getterMethod.name)
        }
      })
  }

  def findDeclaredProperty(name: String): BeanProperty = declaredProperties.find(property => property.name == name).orNull

  def findProperty(name : String) : BeanProperty = properties.find(property => property.name == name).orNull

  override lazy val declaredAnnotations: Array[Annotation] = underlying.declaredAnnotations
  override lazy val annotations: Array[Annotation] = underlying.annotations
}
