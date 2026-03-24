package app.support

import app.domain.core.Link
import jfx.form.{ErrorResponse, ErrorResponseException, Model}
import org.scalajs.dom.{RequestInit, fetch, window}

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

object Api {

  given ExecutionContext = ExecutionContext.global

  def get[M <: Model[M]](url: String): Future[M] =
    requestJson("GET", url).map(deserialize[M])

  def post[M <: Model[M]](url: String, body: Any = null): Future[M] =
    requestJson("POST", url, body).map(deserialize[M])

  def put[M <: Model[M]](url: String, body: Any = null): Future[M] =
    requestJson("PUT", url, body).map(deserialize[M])

  def delete(url: String, body: Any = null): Future[Unit] =
    requestJson("DELETE", url, body).map(_ => ())

  def invokeLink[M <: Model[M]](link: Link, body: Any = null): Future[M] =
    requestJson(link.method, Navigation.prefixedServiceUrl(link.url), body).map(deserialize[M])

  def postText(url: String, body: String): Future[String] =
    requestText("POST", url, if (body == null) js.undefined else body.asInstanceOf[js.Any])

  def requestJson(method: String, url: String, body: Any = null): Future[js.Any] =
    requestText(method, url, serializeBody(body)).map { text =>
      if (text.trim.isEmpty) null
      else js.JSON.parse(text)
    }

  private def deserialize[M <: Model[M]](raw: js.Any): M =
    if (raw == null || js.isUndefined(raw)) {
      null.asInstanceOf[M]
    } else {
      AppJson.mapper.deserialize(raw.asInstanceOf[js.Dynamic]).asInstanceOf[M]
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
      headers = headers,
      credentials = "include"
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
        } else if (response.status == 403) {
          Navigation.redirectToLogin()
          Future.failed(RuntimeException("Request was rejected with 403"))
        } else if (response.status == 400) {
          Future.failed(new ErrorResponseException(parseErrorResponses(text)))
        } else {
          Future.failed(RuntimeException(s"HTTP ${response.status}: $text"))
        }
      }
    }
  }

  private def parseErrorResponses(text: String): Seq[ErrorResponse] =
    if (text == null || text.trim.isEmpty) {
      Seq.empty
    } else {
      val raw = js.JSON.parse(text)
      if (!raw.isInstanceOf[js.Array[?]]) {
        Seq.empty
      } else {
        raw.asInstanceOf[js.Array[js.Dynamic]].toSeq.map { entry =>
          val message =
            entry.selectDynamic("message").asInstanceOf[js.UndefOr[String]].getOrElse("")
          val path =
            entry.selectDynamic("path").asInstanceOf[js.UndefOr[js.Array[Any]]].getOrElse(js.Array())

          new ErrorResponse(message, path)
        }
      }
    }

  def logFailure(label: String, error: Throwable): Unit =
    window.console.error(s"$label failed: ${error.getMessage}")

  def fireAndForget(label: String)(future: Future[?]): Unit =
    future.failed.foreach(error => logFailure(label, error))
}
