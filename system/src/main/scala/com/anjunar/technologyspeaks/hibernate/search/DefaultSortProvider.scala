package com.anjunar.technologyspeaks.hibernate.search

import jakarta.persistence.criteria.{From, JoinType, Order, Path}
import org.hibernate.query.SortDirection
import org.springframework.stereotype.Component

@Component
class DefaultSortProvider[E] extends SortProvider[java.util.List[String], E] {

  override def sort(context: Context[java.util.List[String], E]): java.util.List[Order] = {
    val sortValues = readSortValues(context)
    if (sortValues == null || sortValues.isEmpty) {
      val selection = context.selection
      if (!selection.isEmpty) {
        java.util.List.of(context.builder.desc(selection.get(0)))
      } else {
        new java.util.ArrayList[Order]()
      }
    } else {
      val orders = new java.util.ArrayList[Order](sortValues.size)
      val iterator = sortValues.iterator()
      while (iterator.hasNext) {
        val spec = parseSpec(iterator.next())
        if (spec != null) {
          val expression = resolvePath(context.root.asInstanceOf[Path[?]], spec.path)
          if (expression != null) {
            spec.direction match {
              case SortDirection.DESCENDING =>
                orders.add(context.builder.desc(expression))
              case SortDirection.ASCENDING =>
                orders.add(context.builder.asc(expression))
            }
          }
        }
      }
      orders
    }
  }

  private def readSortValues(context: Context[java.util.List[String], E]): java.util.List[String] = {
    val rawValue = context.value.asInstanceOf[Any]

    rawValue match {
      case list: java.util.List[?] =>
        val result = new java.util.ArrayList[String]()
        val iterator = list.iterator()
        while (iterator.hasNext) {
          iterator.next() match {
            case value: String => result.add(value)
            case _ =>
          }
        }
        result
      case search: AbstractSearch =>
        search.sort
      case _ =>
        null
    }
  }

  private def parseSpec(raw: String): SortSpec = {
    val trimmed = raw.trim
    if (trimmed.isEmpty) {
      return null
    }

    var working = trimmed
    var directionFromPrefix: SortDirection = null

    if (working.startsWith("-")) {
      directionFromPrefix = SortDirection.DESCENDING
      working = working.stripPrefix("-").stripLeading()
    } else if (working.startsWith("+")) {
      directionFromPrefix = SortDirection.ASCENDING
      working = working.stripPrefix("+").stripLeading()
    }

    val delimiterIndex = working.lastIndexOf(':')
    val path =
      if (delimiterIndex >= 0) working.substring(0, delimiterIndex).trim
      else working

    if (path.isEmpty) {
      return null
    }

    val directionText =
      if (delimiterIndex >= 0) working.substring(delimiterIndex + 1).trim
      else ""

    val direction =
      if (directionFromPrefix != null) directionFromPrefix
      else {
        val interpreted = interpretDirection(directionText)
        if (interpreted != null) interpreted else SortDirection.ASCENDING
      }

    SortSpec(path, direction)
  }

  private def interpretDirection(raw: String): SortDirection = {
    if (raw.isBlank) {
      return null
    }

    try {
      SortDirection.interpret(raw.trim)
    } catch {
      case _: IllegalArgumentException => null
    }
  }

  private def resolvePath(root: Path[?], rawPath: String): jakarta.persistence.criteria.Expression[?] = {
    val segments = rawPath.split("\\.").iterator.map(_.trim).filter(_.nonEmpty).toVector
    if (segments.isEmpty) {
      null
    } else {
      var current: Path[?] = root
      var index = 0
      while (index < segments.length) {
        val segment = segments(index)
        val isLast = index == segments.length - 1
        current =
          if (isLast) current.get[Any](segment)
          else current match {
            case from: From[?, ?] =>
              try from.join[Any, Any](segment, JoinType.LEFT)
              catch {
                case _: IllegalArgumentException => from.get[Any](segment)
              }
            case other            => other.get[Any](segment)
          }
        index += 1
      }
      current
    }
  }

  private case class SortSpec(path: String, direction: SortDirection)

}
