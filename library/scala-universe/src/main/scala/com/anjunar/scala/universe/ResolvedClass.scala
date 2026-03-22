package com.anjunar.scala.universe


import com.anjunar.scala.universe.annotations.Annotated
import com.anjunar.scala.universe.members.{ResolvedConstructor, ResolvedField, ResolvedMethod}
import com.google.common.reflect.TypeToken

import java.lang.annotation.Annotation
import java.lang.reflect.{ParameterizedType, Type}
import scala.annotation.targetName
import scala.collection.mutable.ArrayBuffer

class ResolvedClass(val underlying : Type) extends Annotated {

  lazy val name : String = raw.getSimpleName

  lazy val fullName : String = raw.getName

  lazy val raw : Class[?] = TypeResolver.rawType(underlying)

  lazy val hierarchy : Array[ResolvedClass] = {
    val result = ArrayBuffer[ResolvedClass]()
    var cursor : Type = underlying
    while (cursor != null) {
      result.addOne(TypeResolver.resolve(TypeToken.of(underlying).resolveType(cursor).getType))
      val rawClass = TypeResolver.rawType(TypeToken.of(underlying).resolveType(cursor).getType)
      rawClass.getGenericInterfaces.foreach(interface => result.addOne(TypeResolver.resolve(TypeToken.of(underlying).resolveType(interface).getType)))
      cursor = rawClass.getGenericSuperclass
      if (cursor == classOf[Object]) {
        cursor = null
      }
    }
    result.toArray
  }

  lazy val declaredFields : Array[ResolvedField] = raw.getDeclaredFields.map(field => new ResolvedField(field, this))

  lazy val declaredConstructors : Array[ResolvedConstructor] = raw.getDeclaredConstructors.map(constructor => new ResolvedConstructor(constructor, this))

  lazy val declaredMethods : Array[ResolvedMethod] = raw.getDeclaredMethods.map(method => new ResolvedMethod(method, this))

  lazy val fields : Array[ResolvedField] = {
    val allFields = hierarchy.flatMap(aClass => aClass.declaredFields)
    val hidden = allFields.flatMap(field => field.hidden)
    allFields.filter(field => ! hidden.contains(field))
  }

  lazy val constructors : Array[ResolvedConstructor] = hierarchy.flatMap(aClass => aClass.declaredConstructors)

  lazy val methods : Array[ResolvedMethod] = {
    val allMethods = hierarchy.flatMap(aClass => aClass.declaredMethods)
    val overridden = allMethods.flatMap(method => method.overrides)
    allMethods.filter(method => ! overridden.contains(method))
  }

  lazy val typeArguments : Array[ResolvedClass] = underlying match
    case parameterizedType: ParameterizedType => parameterizedType.getActualTypeArguments.map(aType => TypeResolver.resolve(aType))
    case _ => Array()

  @targetName("<:<")
  def <:<(aClass: ResolvedClass): Boolean = TypeToken.of(TypeToken.of(underlying).wrap().getType).isSubtypeOf(aClass.underlying)
  
  def findDeclaredField(name : String) : ResolvedField =
    try {
      val field = raw.getDeclaredField(name)
      declaredFields.find(resolvedField => resolvedField == field).orNull
    } catch {
      case e : NoSuchFieldException => null
    }

  def findDeclaredConstructor(args : Class[?]*) : ResolvedConstructor = {
    try {
      val constructor = raw.getDeclaredConstructor(args *)
      declaredConstructors.find(resolvedConstructor => resolvedConstructor.underlying == constructor).orNull
    } catch {
      case e : NoSuchMethodException => null
    }
  }
  
  def findDeclaredMethod(name : String, args : Class[?]*) : ResolvedMethod =
    try {
      val method = raw.getDeclaredMethod(name, args *)
      declaredMethods.find(resolvedMethod => resolvedMethod.underlying == method).orNull
    } catch {
      case e : NoSuchMethodException => null
    }


  def findField(name: String): ResolvedField =
    try {
      fields.find(resolvedField => resolvedField.name == name).orNull
    } catch {
      case e : NoSuchFieldException => null
    }

  def findConstructor(args: Class[?]*): ResolvedConstructor = {
    try {
      val constructor = raw.getConstructor(args *)
      constructors.find(resolvedConstructor => resolvedConstructor.underlying == constructor).orNull
    } catch {
      case e: NoSuchMethodException => null
    }
  }

  def findMethod(name: String, args: Class[?]*): ResolvedMethod =
    try {
      val method = raw.getMethod(name, args *)
      methods.find(resolvedMethod => resolvedMethod.underlying == method).orNull
    } catch {
      case e : NoSuchMethodException => null
    }

  override lazy val declaredAnnotations: Array[Annotation] = raw.getDeclaredAnnotations

  override lazy val annotations: Array[Annotation] = hierarchy.flatMap(aClass => aClass.declaredAnnotations)


  private def canEqual(other: Any): Boolean = other.isInstanceOf[ResolvedClass]
  
  override def equals(other: Any): Boolean = other match
    case that: ResolvedClass =>
      that.canEqual(this) &&
        underlying == that.underlying
    case _ => false
  
  override def hashCode(): Int =
    val state = Seq(underlying)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)


  override def toString = s"ResolvedClass($name)"
}
