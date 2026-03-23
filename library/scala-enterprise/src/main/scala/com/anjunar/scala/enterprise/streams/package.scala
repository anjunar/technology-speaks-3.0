package com.anjunar.scala.enterprise

import java.util
import java.util.stream.{Collectors, Stream as JavaStream}

import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*

package object streams {

  private object StreamInternals {

    def toArrayList[A](stream: JavaStream[A]): util.ArrayList[A] =
      stream.collect(Collectors.toCollection(() => new util.ArrayList[A]()))

    def streamOf[A](values: IterableOnce[A]): JavaStream[A] = {
      val buffer = new util.ArrayList[A]()
      values.iterator.foreach(buffer.add)
      buffer.stream()
    }

    def toScala[A](optional: java.util.Optional[A]): Option[A] =
      if optional.isPresent then Some(optional.get()) else None

    def toScalaList[A](stream: JavaStream[A]): List[A] =
      toArrayList(stream).asScala.toList
  }

  extension [A](stream: JavaStream[A]) {

    def mkString: String =
      stream.map(String.valueOf).collect(Collectors.joining())

    def mkString(separator: String): String =
      stream.map(String.valueOf).collect(Collectors.joining(separator))

    def mkString(start: String, separator: String, end: String): String =
      stream.map(String.valueOf).collect(Collectors.joining(separator, start, end))

    def filterNot(predicate: A => Boolean): JavaStream[A] =
      stream.filter(value => !predicate(value))

    def collectFirst[B](partialFunction: PartialFunction[A, B]): Option[B] = {
      val iterator = stream.iterator()
      var result: Option[B] = None

      while iterator.hasNext && result.isEmpty do {
        val value = iterator.next()
        if partialFunction.isDefinedAt(value) then result = Some(partialFunction(value))
      }

      result
    }

    def headOption: Option[A] =
      StreamInternals.toScala(stream.findFirst())

    def lastOption: Option[A] = {
      val iterator = stream.iterator()
      var result: Option[A] = None

      while iterator.hasNext do result = Some(iterator.next())

      result
    }

    def reduceOption(operator: (A, A) => A): Option[A] =
      StreamInternals.toScala(stream.reduce((left, right) => operator(left, right)))

    def foldLeft[B](initial: B)(operator: (B, A) => B): B = {
      val iterator = stream.iterator()
      var result = initial

      while iterator.hasNext do result = operator(result, iterator.next())

      result
    }

    def foldRight[B](initial: B)(operator: (A, B) => B): B = {
      val values = StreamInternals.toArrayList(stream)
      var index = values.size() - 1
      var result = initial

      while index >= 0 do {
        result = operator(values.get(index), result)
        index -= 1
      }

      result
    }

    def scanLeft[B](initial: B)(operator: (B, A) => B): JavaStream[B] = {
      val values = new util.ArrayList[B]()
      val iterator = stream.iterator()
      var current = initial

      values.add(current)

      while iterator.hasNext do {
        current = operator(current, iterator.next())
        values.add(current)
      }

      values.stream()
    }

    def scanRight[B](initial: B)(operator: (A, B) => B): JavaStream[B] = {
      val source = StreamInternals.toArrayList(stream)
      val values = new util.ArrayList[B](source.size() + 1)
      var current = initial

      values.add(current)

      var index = source.size() - 1
      while index >= 0 do {
        current = operator(source.get(index), current)
        values.add(0, current)
        index -= 1
      }

      values.stream()
    }

    def zipWithIndex: JavaStream[(A, Long)] = {
      val values = new util.ArrayList[(A, Long)]()
      val iterator = stream.iterator()
      var index = 0L

      while iterator.hasNext do {
        values.add((iterator.next(), index))
        index += 1
      }

      values.stream()
    }

    def zip[B](other: JavaStream[B]): JavaStream[(A, B)] = {
      val left = stream.iterator()
      val right = other.iterator()
      val values = new util.ArrayList[(A, B)]()

      while left.hasNext && right.hasNext do values.add((left.next(), right.next()))

      values.stream()
    }

    def zipAll[B](other: JavaStream[B], thisElem: A, thatElem: B): JavaStream[(A, B)] = {
      val left = stream.iterator()
      val right = other.iterator()
      val values = new util.ArrayList[(A, B)]()

      while left.hasNext || right.hasNext do {
        val leftValue = if left.hasNext then left.next() else thisElem
        val rightValue = if right.hasNext then right.next() else thatElem
        values.add((leftValue, rightValue))
      }

      values.stream()
    }

    def grouped(size: Int): JavaStream[List[A]] = {
      require(size > 0, "size must be greater than zero")

      val iterator = stream.iterator()
      val groups = new util.ArrayList[List[A]]()

      while iterator.hasNext do {
        val buffer = List.newBuilder[A]
        var current = 0

        while current < size && iterator.hasNext do {
          buffer += iterator.next()
          current += 1
        }

        groups.add(buffer.result())
      }

      groups.stream()
    }

    def sliding(size: Int, step: Int = 1): JavaStream[List[A]] = {
      require(size > 0, "size must be greater than zero")
      require(step > 0, "step must be greater than zero")

      val values = StreamInternals.toArrayList(stream)
      val windows = new util.ArrayList[List[A]]()
      var index = 0

      while index < values.size() do {
        val end = Math.min(index + size, values.size())
        windows.add(values.subList(index, end).asScala.toList)
        index += step
      }

      windows.stream()
    }

    def takeRight(count: Int): JavaStream[A] = {
      val safeCount = Math.max(count, 0)
      val values = StreamInternals.toArrayList(stream)
      val from = Math.max(values.size() - safeCount, 0)
      values.subList(from, values.size()).stream()
    }

    def dropRight(count: Int): JavaStream[A] = {
      val safeCount = Math.max(count, 0)
      val values = StreamInternals.toArrayList(stream)
      val until = Math.max(values.size() - safeCount, 0)
      values.subList(0, until).stream()
    }

    def tail: JavaStream[A] =
      stream.skip(1)

    def init: JavaStream[A] =
      dropRight(1)

    def partition(predicate: A => Boolean): (JavaStream[A], JavaStream[A]) = {
      val matching = new util.ArrayList[A]()
      val rest = new util.ArrayList[A]()
      val iterator = stream.iterator()

      while iterator.hasNext do {
        val value = iterator.next()
        if predicate(value) then matching.add(value)
        else rest.add(value)
      }

      (matching.stream(), rest.stream())
    }

    def groupBy[K](classifier: A => K): Map[K, List[A]] = {
      val groups = new util.LinkedHashMap[K, util.ArrayList[A]]()
      val iterator = stream.iterator()

      while iterator.hasNext do {
        val value = iterator.next()
        val key = classifier(value)
        val buffer = groups.computeIfAbsent(key, _ => new util.ArrayList[A]())
        buffer.add(value)
      }

      groups.asScala.iterator.map((key, values) => key -> values.asScala.toList).toMap
    }

    def partitionMap[L, R](classifier: A => Either[L, R]): (JavaStream[L], JavaStream[R]) = {
      val left = new util.ArrayList[L]()
      val right = new util.ArrayList[R]()
      val iterator = stream.iterator()

      while iterator.hasNext do {
        classifier(iterator.next()) match {
          case Left(leftValue) => left.add(leftValue)
          case Right(rightValue) => right.add(rightValue)
        }
      }

      (left.stream(), right.stream())
    }

    def findLast(predicate: A => Boolean): Option[A] = {
      val iterator = stream.iterator()
      var result: Option[A] = None

      while iterator.hasNext do {
        val value = iterator.next()
        if predicate(value) then result = Some(value)
      }

      result
    }

    def distinctBy[B](selector: A => B): JavaStream[A] = {
      val seen = new util.HashSet[B]()
      stream.filter(value => seen.add(selector(value)))
    }

    def minBy[B](selector: A => B)(using ordering: Ordering[B]): Option[A] = {
      val iterator = stream.iterator()
      var result: Option[A] = None
      var current: Option[B] = None

      while iterator.hasNext do {
        val value = iterator.next()
        val projected = selector(value)

        if current.forall(ordering.gt(_, projected)) then {
          current = Some(projected)
          result = Some(value)
        }
      }

      result
    }

    def maxBy[B](selector: A => B)(using ordering: Ordering[B]): Option[A] = {
      val iterator = stream.iterator()
      var result: Option[A] = None
      var current: Option[B] = None

      while iterator.hasNext do {
        val value = iterator.next()
        val projected = selector(value)

        if current.forall(ordering.lt(_, projected)) then {
          current = Some(projected)
          result = Some(value)
        }
      }

      result
    }

    def span(predicate: A => Boolean): (JavaStream[A], JavaStream[A]) = {
      val values = StreamInternals.toArrayList(stream)
      val prefix = new util.ArrayList[A]()
      val suffix = new util.ArrayList[A]()
      var inSuffix = false

      values.forEach { value =>
        if !inSuffix && predicate(value) then prefix.add(value)
        else {
          inSuffix = true
          suffix.add(value)
        }
      }

      (prefix.stream(), suffix.stream())
    }

    def splitAt(index: Int): (JavaStream[A], JavaStream[A]) = {
      val values = StreamInternals.toArrayList(stream)
      val splitIndex = Math.max(0, Math.min(index, values.size()))

      (values.subList(0, splitIndex).stream(), values.subList(splitIndex, values.size()).stream())
    }

    def padTo(length: Int, element: A): JavaStream[A] = {
      val values = StreamInternals.toArrayList(stream)

      while values.size() < length do values.add(element)

      values.stream()
    }

    def appended(element: A): JavaStream[A] =
      JavaStream.concat(stream, JavaStream.of(element))

    def prepended(element: A): JavaStream[A] =
      JavaStream.concat(JavaStream.of(element), stream)

    def appendedAll(elements: IterableOnce[A]): JavaStream[A] =
      JavaStream.concat(stream, StreamInternals.streamOf(elements))

    def prependedAll(elements: IterableOnce[A]): JavaStream[A] =
      JavaStream.concat(StreamInternals.streamOf(elements), stream)

    def ++(other: JavaStream[A]): JavaStream[A] =
      JavaStream.concat(stream, other)

    def corresponds[B](other: JavaStream[B])(predicate: (A, B) => Boolean): Boolean = {
      val left = stream.iterator()
      val right = other.iterator()

      @tailrec
      def loop(): Boolean = {
        if left.hasNext != right.hasNext then false
        else if !left.hasNext then true
        else if predicate(left.next(), right.next()) then loop()
        else false
      }

      loop()
    }

    def sameElements(other: JavaStream[A]): Boolean =
      corresponds(other)(_ == _)

    def toScalaList: List[A] =
      StreamInternals.toScalaList(stream)
  }
}