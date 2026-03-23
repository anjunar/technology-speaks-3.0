package com.anjunar.scala.enterprise.streams

import java.util.stream.Stream as JavaStream

import org.scalatest.funsuite.AnyFunSuite

class JavaStreamSpec extends AnyFunSuite {

  test("filterNot/distinctBy provide scala-like intermediate operations") {
    val result = JavaStream
      .of(1, 2, 2, 3, 4)
      .filterNot(_ % 2 == 0)
      .distinctBy(_ % 2)
      .toScalaList

    assert(result == List(1))
  }

  test("fold and scan operations keep scala semantics") {
    val folded = JavaStream.of(1, 2, 3, 4).foldLeft(0)(_ + _)
    val scanned = JavaStream.of(1, 2, 3).scanLeft(0)(_ + _).toScalaList
    val scanRight = JavaStream.of(1, 2, 3).scanRight(0)(_ + _).toScalaList

    assert(folded == 10)
    assert(scanned == List(0, 1, 3, 6))
    assert(scanRight == List(6, 5, 3, 0))
  }

  test("grouped sliding and partition materialize the expected subsets") {
    val grouped = JavaStream.of(1, 2, 3, 4, 5).grouped(2).toScalaList
    val sliding = JavaStream.of(1, 2, 3, 4).sliding(3, 2).toScalaList
    val (left, right) = JavaStream.of(1, 2, 3, 4, 5).partition(_ % 2 == 0)

    assert(grouped == List(List(1, 2), List(3, 4), List(5)))
    assert(sliding == List(List(1, 2, 3), List(3, 4)))
    assert(left.toScalaList == List(2, 4))
    assert(right.toScalaList == List(1, 3, 5))
  }

  test("zip utilities and sequence comparisons work like scala collections") {
    val zipped = JavaStream.of("a", "b", "c").zipWithIndex.toScalaList
    val zipAll = JavaStream.of(1, 2).zipAll(JavaStream.of("x"), -1, "?").toScalaList
    val corresponds = JavaStream.of(1, 2, 3).corresponds(JavaStream.of("1", "2", "3"))(_.toString == _)

    assert(zipped == List(("a", 0L), ("b", 1L), ("c", 2L)))
    assert(zipAll == List((1, "x"), (2, "?")))
    assert(corresponds)
  }

  test("terminal helpers expose scala-style optional and string operations") {
    val head = JavaStream.of("alpha", "beta").headOption
    val last = JavaStream.of("alpha", "beta", "gamma").lastOption
    val grouped = JavaStream.of("a", "bb", "c").groupBy(_.length)
    val collected = JavaStream.of("a", "bb", "ccc").collectFirst { case value if value.length == 2 => value }
    val text = JavaStream.of("a", "b", "c").mkString("[", ",", "]")

    assert(head.contains("alpha"))
    assert(last.contains("gamma"))
    assert(grouped == Map(1 -> List("a", "c"), 2 -> List("bb")))
    assert(collected.contains("bb"))
    assert(text == "[a,b,c]")
  }

  test("tail init splitAt and take/drop right behave like scala collections") {
    val tail = JavaStream.of(1, 2, 3).tail.toScalaList
    val init = JavaStream.of(1, 2, 3).init.toScalaList
    val takeRight = JavaStream.of(1, 2, 3, 4).takeRight(2).toScalaList
    val dropRight = JavaStream.of(1, 2, 3, 4).dropRight(2).toScalaList
    val (left, right) = JavaStream.of(1, 2, 3, 4).splitAt(2)

    assert(tail == List(2, 3))
    assert(init == List(1, 2))
    assert(takeRight == List(3, 4))
    assert(dropRight == List(1, 2))
    assert(left.toScalaList == List(1, 2))
    assert(right.toScalaList == List(3, 4))
  }
}