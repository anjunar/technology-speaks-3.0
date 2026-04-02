package app.support

import app.domain.core.Link
import reflect.TypeDescriptor
import reflect.macros.ReflectMacros.reflectType
import jfx.form.{ErrorResponse, ErrorResponseException}
import jfx.json.JsonMapper
import org.scalajs.dom
import org.scalajs.dom.{RequestInit, fetch, window}

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.JSON

object Api {

  given ExecutionContext = ExecutionContext.global

  def get(url: String): Future[js.Any] =
    requestJson("GET", url, null)

  def post(url: String, body: Any): Future[js.Any] =
    requestJson("POST", url, body)

  def post(url: String): Future[js.Any] =
    requestJson("POST", url, null)

  def put(url: String, body: Any): Future[js.Any] =
    requestJson("PUT", url, body)

  def delete(url: String, body: Any): Future[Unit] =
    requestJson("DELETE", url, body).map(_ => ())

  def postText(url: String, body: String): Future[String] =
    requestText("POST", url, body)

  def invokeLink(link: Link, body: Any): Future[js.Any] =
    requestJson(link.method, link.url, body)

  def invokeLink(link: Link): Future[js.Any] =
    requestJson(link.method, link.url, null)

  def requestJson(method: String, url: String, body: Any): Future[js.Any] =
    requestText(method, url, body).map { text =>
      if (text.isEmpty) null
      else js.JSON.parse(text)
    }

  def requestJson(method: String, url: String): Future[js.Any] =
    requestText(method, url, null).map { text =>
      if (text.isEmpty) null
      else js.JSON.parse(text)
    }

  inline def deserialize[M](raw: js.Any): M =
    if (raw == null || js.isUndefined(raw)) {
      null.asInstanceOf[M]
    } else {
      AppJson.mapper.deserialize(raw.asInstanceOf[js.Dynamic], reflectType[M]).asInstanceOf[M]
    }

  private def serializeBody(body: Any): js.UndefOr[dom.BodyInit] =
    body match {
      case null =>
        js.undefined
      case text: String =>
        text.asInstanceOf[dom.BodyInit]
      case other =>
        other.asInstanceOf[dom.BodyInit]
    }

  private def requestText(
    methodArg: String,
    urlArg: String,
    bodyArg: Any = null
  ): Future[String] = {
    val init = new RequestInit {
      method = methodArg.asInstanceOf[dom.HttpMethod]
      body = serializeBody(bodyArg)
      headers = js.Dictionary("Accept" -> "application/json")
    }

    fetch(urlArg, init).toFuture.flatMap { response =>
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
          Future.failed(new ErrorResponseException(AppJson.mapper.deserializeArray(JSON.parse(text).asInstanceOf[js.Array[js.Dynamic]], reflectType[ErrorResponse])))
        } else {
          Future.failed(RuntimeException(s"HTTP ${response.status}: $text"))
        }
      }
    }
  }

  def logFailure(label: String, error: Throwable): Unit =
    window.console.error(s"$label failed: ${error.getMessage}")

  def fireAndForget(label: String)(future: Future[?]): Unit =
    future.failed.foreach(error => logFailure(label, error))
}
