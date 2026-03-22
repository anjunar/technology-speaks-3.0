package com.anjunar.scala.universe

import scala.beans.BeanProperty
import scala.collection.mutable.ArrayBuffer

class AbstractContainerNode[C <: AbstractNode[?]] extends AbstractNode[String]("test") {

  val children: Seq[C] = Seq.empty


}
