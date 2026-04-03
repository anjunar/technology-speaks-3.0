package app.support

import app.domain.core.Link
import jfx.form.{ErrorResponse, ErrorResponseException}
import jfx.json.JsonMapper
import org.scalajs.dom
import org.scalajs.dom.{RequestInit, fetch, window}
import reflect.TypeDescriptor
import reflect.macros.ReflectMacros.reflectType

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.JSON

object Api {

  given ExecutionContext = ExecutionContext.global

  def request(url: String): RequestBuilder =
    new RequestBuilder(url)

  def link(link: Link): LinkRequestBuilder =
    new LinkRequestBuilder(link)

  final class RequestBuilder(val url: String) {

    def get: PreparedRequest =
      new PreparedRequest("GET", url, null)

    def post: PreparedRequest =
      new PreparedRequest("POST", url, null)

    inline def post[B](body: B): PreparedRequest =
      new PreparedRequest("POST", url, encodeBody(body, reflectType[B]))

    inline def put[B](body: B): PreparedRequest =
      new PreparedRequest("PUT", url, encodeBody(body, reflectType[B]))

    inline def delete[B](body: B): PreparedRequest =
      new PreparedRequest("DELETE", url, encodeBody(body, reflectType[B]))
  }

  final class LinkRequestBuilder(link: Link) {

    private val url = "/service" + link.url

    def invoke: PreparedRequest =
      new PreparedRequest(link.method, url, null)

    inline def invoke[B](body: B): PreparedRequest =
      new PreparedRequest(link.method, url, encodeBody(body, reflectType[B]))
  }

  final class PreparedRequest(
    val method: String,
    val url: String,
    val body: js.Any
  ) {

    inline def read[R]: Future[R] =
      requestInternal[R](method, url, body, reflectType[R])

    inline def raw[R]: Future[R] =
      requestInternal[R](method, url, body, reflectType[R])

    inline def unit: Future[Unit] =
      requestInternal[Unit](method, url, body, reflectType[Unit])

    def text: Future[String] =
      requestText(method, url, body)
  }

  private def requestText(
    methodArg: String,
    urlArg: String,
    bodyArg: js.Any = null
  ): Future[String] = {
    val headers = js.Dictionary(
      "Content-Type" -> "application/json",
      "Accept" -> "application/json"
    )

    val body =
      if (bodyArg == null || js.isUndefined(bodyArg)) {
        null
      } else if (js.typeOf(bodyArg) == "string") {
        bodyArg
      } else {
        js.JSON.stringify(bodyArg)
      }

    val init = js.Dynamic.literal(
      method = methodArg.asInstanceOf[dom.HttpMethod],
      body = body.asInstanceOf[js.Any],
      headers = headers,
    )

    fetch(urlArg, init.asInstanceOf[RequestInit]).toFuture.flatMap { response =>
      response.text().toFuture.flatMap { text =>
        if (response.ok) {
          Future.successful(text)
        } else if (response.status == 428) {
          Navigation.navigate("/security/confirm", replace = true)
          Future.failed(RuntimeException("Request was rejected with 428"))
        } else if (response.status == 403) {
          Navigation.redirectToLogin()
          Future.failed(RuntimeException("Request was rejected with 403"))
        } else if (response.status == 400) {
          Future.failed(new ErrorResponseException(JsonMapper.deserialize(JSON.parse(text), reflectType[ErrorResponse])))
        } else {
          Future.failed(RuntimeException(s"HTTP ${response.status}: $text"))
        }
      }
    }
  }

  private def requestInternal[R](
    method: String,
    url: String,
    body: js.Any,
    responseType: TypeDescriptor
  ): Future[R] =
    requestText(method, url, body).map(text => decodeResponse[R](text, responseType))

  private def encodeBody[B](body: B, bodyType: TypeDescriptor): js.Any =
    if (body == null) {
      null
    } else if (isRawJsonType(bodyType) || isPrimitiveType(bodyType.typeName)) {
      body.asInstanceOf[js.Any]
    } else {
      JsonMapper.serialize(body, bodyType)
    }

  private def decodeResponse[R](text: String, responseType: TypeDescriptor): R =
    if (isUnitType(responseType)) {
      ().asInstanceOf[R]
    } else if (text.isEmpty) {
      null.asInstanceOf[R]
    } else {
      val raw = JSON.parse(text)
      if (isRawJsonType(responseType)) {
        raw.asInstanceOf[R]
      } else {
        JsonMapper.deserialize(raw.asInstanceOf[js.Dynamic], responseType).asInstanceOf[R]
      }
    }

  private def isUnitType(descriptor: TypeDescriptor): Boolean =
    descriptor.typeName == "scala.Unit"

  private def isPrimitiveType(typeName: String): Boolean =
    typeName == "scala.Predef.String" ||
      typeName == "java.lang.String" ||
      typeName == "scala.Boolean" ||
      typeName == "boolean" ||
      typeName == "scala.Int" ||
      typeName == "int" ||
      typeName == "scala.Double" ||
      typeName == "double" ||
      typeName == "scala.Float" ||
      typeName == "float" ||
      typeName == "scala.Long" ||
      typeName == "long" ||
      typeName == "scala.Short" ||
      typeName == "short" ||
      typeName == "scala.Byte" ||
      typeName == "byte" ||
      typeName == "scala.Char" ||
      typeName == "char"

  private def isRawJsonType(descriptor: TypeDescriptor): Boolean =
    descriptor.typeName == "scala.scalajs.js.Any" ||
      descriptor.typeName == "scala.Any" ||
      descriptor.typeName == "scala.scalajs.js.Object" ||
      descriptor.typeName == "scala.scalajs.js.Dynamic"

  def logFailure(label: String, error: Throwable): Unit =
    window.console.error(s"$label failed: ${error.getMessage}")

  def fireAndForget(label: String)(future: Future[?]): Unit =
    future.failed.foreach(error => logFailure(label, error))
}
