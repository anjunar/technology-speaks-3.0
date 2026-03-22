package com.anjunar.scala.universe

import com.anjunar.scala.universe.introspector.BeanIntrospector

object Main {

  def main(args: Array[String]): Unit = {

    val model = BeanIntrospector.createWithType(classOf[TestNode])

    println(model)

  }

}
