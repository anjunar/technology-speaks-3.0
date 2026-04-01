package app.support

import app.domain.core.Link
import jfx.core.meta.Meta
import jfx.form.{ErrorResponse, ErrorResponseException, Model}
import jfx.json.JsonMapper
import org.scalajs.dom.{RequestInit, fetch, window}

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.JSON

object Api {

  given ExecutionContext = ExecutionContext.global

  def get(url: String): Future[js.Any] =
    requestJson("GET", url)

  def post(url: String, body: Any = null): Future[js.Any] =
    requestJson("POST", url, body)

  def put(url: String, body: Any = null): Future[js.Any] =
    requestJson("PUT", url, body)

  def delete(url: String, body: Any = null): Future[Unit] =
    requestJson("DELETE", url, body).map(_ => ())

  def invokeLink(link: Link, body: Any = null): Future[js.Any] =
    requestJson(link.method, Navigation.prefixedServiceUrl(link.url), body)

  def postText(url: String, body: String): Future[String] =
    requestText("POST", url, if (body == null) js.undefined else body.asInstanceOf[js.Any])

  def requestJson(method: String, url: String, body: Any = null): Future[js.Any] =
    requestText(method, url, serializeBody(body)).map { text =>
      if (text.trim.isEmpty) null
      else js.JSON.parse(text)
    }

  def deserialize[M <: Model[M]](raw: js.Any, meta: Meta[M]): M =
    if (raw == null || js.isUndefined(raw)) {
      null.asInstanceOf[M]
    } else {
      AppJson.mapper.deserialize(raw.asInstanceOf[js.Dynamic], meta).asInstanceOf[M]
    }

  private def serializeBody(body: Any): js.UndefOr[js.Any] =
    body match {
      case null =>
        js.undefined
      case text: String =>
        text.asInstanceOf[js.Any]
      case model: Model[?] =>
        AppJson.mapper.serialize(model)
      case other =>
        other.asInstanceOf[js.Any]
    }

  private def serializeModel(model: Model[?]): js.Dynamic =
    AppJson.mapper.serialize(model)

  private def requestText(
    method: String,
    url: String,
    body: js.UndefOr[js.Any]
  ): Future[String] = {
    val headers = js.Dictionary(
      "Content-Type" -> "application/json",
      "Accept" -> "application/json"
    )

    val init = js.Dynamic.literal(
      method = method,
      headers = headers
    )

    if (!js.isUndefined(body)) {
      val payload =
        body.asInstanceOf[Any] match {
          case text: String => text
          case other        => js.JSON.stringify(other.asInstanceOf[js.Any])
        }

      init.updateDynamic("body")(payload)
    }

    fetch(url, init.asInstanceOf[RequestInit]).toFuture.flatMap { response =>
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
          Future.failed(new ErrorResponseException(AppJson.mapper.deserializeArray(JSON.parse(text), ErrorResponse.meta)))
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
