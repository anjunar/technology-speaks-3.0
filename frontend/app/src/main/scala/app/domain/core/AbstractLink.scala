package app.domain.core

trait AbstractLink {
  def rel: String

  def url: String

  def method: String

  def name: String

  def icon: String

}
