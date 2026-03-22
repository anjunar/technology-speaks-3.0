package app.support

import app.domain.core.Link
import org.scalajs.dom.{Event, URLSearchParams, window}

import scala.scalajs.js

object Navigation {

  def currentUrl: String =
    s"${window.location.pathname}${window.location.search}"

  def navigate(path: String, replace: Boolean = false): Unit = {
    if (replace) {
      window.history.replaceState(null, "", path)
    } else {
      window.history.pushState(null, "", path)
    }

    window.dispatchEvent(new Event("popstate"))
  }

  def queryParam(name: String): Option[String] = {
    val params = new URLSearchParams(window.location.search)
    Option(params.get(name))
  }

  def prefixedServiceUrl(url: String): String =
    if (url.startsWith("/service/")) url
    else if (url.startsWith("/")) s"/service$url"
    else s"/service/$url"

  def redirectToLogin(): Unit = {
    val redirect = js.URIUtils.encodeURIComponent(window.location.pathname)
    
    val platformAvailable = js.Dynamic.global.eval("PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable();")
      .asInstanceOf[Boolean]

    if (!platformAvailable) {
      navigate("/security/login?redirect=${encodeURIComponent(window.location.pathname)}")
    } else {
      navigate("/security/login/options?redirect=${encodeURIComponent(window.location.pathname)}")
    }
  }

  def linkByRel(rel: String, links: IterableOnce[Link]): Option[Link] =
    links.iterator.find(_.rel == rel)
}
