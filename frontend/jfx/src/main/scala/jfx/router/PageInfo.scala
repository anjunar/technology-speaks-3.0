package jfx.router

trait PageInfo {

  def name: String

  def resizable: Boolean = true

  var close: () => Unit = () => ()
}
