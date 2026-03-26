package com.anjunar.technologyspeaks.security

import com.anjunar.technologyspeaks.SpringContext
import jakarta.annotation.security.RolesAllowed
import org.scalatest.funsuite.AnyFunSuite
import org.springframework.context.ApplicationContext
import org.springframework.web.bind.annotation.*
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar

import com.anjunar.technologyspeaks.core.User
import java.util.UUID

class TestController {
  @GetMapping(Array("/test/{id}"))
  @RolesAllowed(Array("ADMIN"))
  def getTest(@PathVariable("id") id: String, @RequestParam("query") query: String): String = s"test-$id-$query"

  @GetMapping(Array("/users/{id}"))
  def getUser(@PathVariable("id") user: User): String = s"user-${user.id}"

  @PostMapping(Array("/create"))
  def createTest(): String = "created"

  @PutMapping(Array("/update/{id}"))
  def updateTest(@PathVariable("id") id: String): String = "updated"

  @DeleteMapping(Array("/delete/{id}"))
  def deleteTest(@PathVariable("id") id: String): String = "deleted"

  @GetMapping(Array("/search/{id}"))
  def searchTest(search: TestSearch): String = "searched"
}

case class TestSearch(id: User)

class LinkBuilderSpec extends AnyFunSuite with MockitoSugar {

  test("LinkBuilder.build should expand variables and generate correct link") {
    val method = classOf[TestController].getMethod("getTest", classOf[String], classOf[String])
    val builder = new LinkBuilder("/test/{id}?q={query}", "custom-rel", "GET", method)
    builder.withVariable("id", "123")
    builder.withVariable("query", "search")
    builder.withId(true)

    val link = builder.build()

    assert(link.url == "/test/123?q=search")
    assert(link.rel == "custom-rel")
    assert(link.method == "GET")
    assert(link.id == "test-getTest")
  }

  test("LinkBuilder.build should use method name as default rel") {
    val method = classOf[TestController].getMethod("createTest")
    val builder = new LinkBuilder("/create", null, "POST", method)
    
    val link = builder.build()
    
    assert(link.rel == "createTest")
  }

  test("LinkBuilder.create macro should extract mapping and parameters") {
    // We need to mock SpringContext to avoid NullPointerException during checkRolesAndGenerate
    val mockContext = mock[ApplicationContext]
    val mockIdentityHolder = mock[IdentityHolder]
    SpringContext.context = mockContext
    when(mockContext.getBean(classOf[IdentityHolder])).thenReturn(mockIdentityHolder)
    when(mockIdentityHolder.hasRole("ADMIN")).thenReturn(true)

    val builder = LinkBuilder.create[TestController](_.getTest("456", "foo"))
    val link = builder.build()

    assert(link.url == "/test/456")
    assert(link.method == "GET")
  }

  test("LinkBuilder.create should return null href if roles are not met") {
    val mockContext = mock[ApplicationContext]
    val mockIdentityHolder = mock[IdentityHolder]
    SpringContext.context = mockContext
    when(mockContext.getBean(classOf[IdentityHolder])).thenReturn(mockIdentityHolder)
    when(mockIdentityHolder.hasRole("ADMIN")).thenReturn(false)

    val builder = LinkBuilder.create[TestController](_.getTest("456", "foo"))
    val link = builder.build()

    assert(link == null)
  }

  test("LinkBuilder.create should handle PutMapping and DeleteMapping") {
    val mockContext = mock[ApplicationContext]
    val mockIdentityHolder = mock[IdentityHolder]
    SpringContext.context = mockContext
    when(mockContext.getBean(classOf[IdentityHolder])).thenReturn(mockIdentityHolder)

    val putBuilder = LinkBuilder.create[TestController](_.updateTest("1"))
    assert(putBuilder.build().url == "/update/1")
    assert(putBuilder.build().method == "PUT")

    val deleteBuilder = LinkBuilder.create[TestController](_.deleteTest("2"))
    assert(deleteBuilder.build().url == "/delete/2")
    assert(deleteBuilder.build().method == "DELETE")
  }

  test("LinkBuilder.create should handle Entity parameters (e.g. User)") {
    val mockContext = mock[ApplicationContext]
    val mockIdentityHolder = mock[IdentityHolder]
    SpringContext.context = mockContext
    when(mockContext.getBean(classOf[IdentityHolder])).thenReturn(mockIdentityHolder)

    val user = new User("JohnDoe")
    val userId = user.id.toString

    val builder = LinkBuilder.create[TestController](_.getUser(user))
    val link = builder.build()

    assert(link.url == s"/users/$userId")
  }

  test("LinkBuilder.create should extract ID from complex objects (Search objects)") {
    val mockContext = mock[ApplicationContext]
    val mockIdentityHolder = mock[IdentityHolder]
    SpringContext.context = mockContext
    when(mockContext.getBean(classOf[IdentityHolder])).thenReturn(mockIdentityHolder)

    val user = new User("JohnDoe")
    val userId = user.id.toString

    val builder = LinkBuilder.create[TestController](_.searchTest(TestSearch(user)))
    val link = builder.build()

    assert(link.url == s"/search/$userId")
  }
}
