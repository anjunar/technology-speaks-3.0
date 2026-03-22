package com.anjunar.technologyspeaks.security

import com.anjunar.json.mapper.schema.Link
import com.anjunar.technologyspeaks.{SpringContext, toKebabCase}
import jakarta.annotation.security.RolesAllowed
import org.springframework.web.bind.annotation.{DeleteMapping, GetMapping, PostMapping, PutMapping}
import org.springframework.web.util.UriComponentsBuilder

import java.lang.reflect.Method

class LinkBuilder(
  val href: String,
  var rel: String,
  val method: String,
  val function: Method
) {

  var withId: Boolean = false

  val variables: java.util.Map[String, Any] = new java.util.HashMap[String, Any]()

  def withVariable(name: String, value: Any): LinkBuilder = {
    variables.put(name, value)
    this
  }

  def withRel(rel: String): LinkBuilder = {
    this.rel = rel
    this
  }

  def withId(value : Boolean): LinkBuilder = {
    withId = value
    this
  }

  def build(): Link = {
    if (rel == null || href == null) {
      return null
    }

    val uriString = UriComponentsBuilder
      .fromPath("/")
      .path(href)
      .buildAndExpand(variables)
      .toUriString

    val linkId =
      if (function == null) null
      else function.getDeclaringClass.getSimpleName.toKebabCase().replace("-controller", "") + "-" + function.getName

    new Link(rel, uriString, method, if withId then linkId else null)
  }

}

object LinkBuilder {

  def create(controllerClass: Class[?], functionName: String): LinkBuilder = {
    val function = controllerClass.getMethods.find(method => method.getName == functionName).orNull
    if (function == null) {
      return generateLinkBuilder(null)
    }

    val rolesAllowed = function.getAnnotation(classOf[RolesAllowed])
    if (rolesAllowed == null) {
      generateLinkBuilder(function)
    } else {
      val identityHolder = SpringContext.getBean(classOf[IdentityHolder])
      if (rolesAllowed.value().exists(role => identityHolder.hasRole(role))) {
        generateLinkBuilder(function)
      } else {
        generateLinkBuilder(null)
      }
    }
  }

  private def generateLinkBuilder(function: Method): LinkBuilder = {
    if (function == null) {
      return new LinkBuilder(null, null, null, null)
    }

    function.getAnnotations.foreach {
      case annotation: GetMapping =>
        return new LinkBuilder(annotation.value()(0), function.getName, "GET", function)
      case annotation: PostMapping =>
        return new LinkBuilder(annotation.value()(0), function.getName, "POST", function)
      case annotation: PutMapping =>
        return new LinkBuilder(annotation.value()(0), function.getName, "PUT", function)
      case annotation: DeleteMapping =>
        return new LinkBuilder(annotation.value()(0), function.getName, "DELETE", function)
      case _ =>
    }

    throw new IllegalArgumentException(s"No Mapping found for function $function")
  }

}
