package app.support

import app.domain.core.Table
import jfx.core.state.{ListProperty, RemoteListProperty}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.JSConverters.*
import scala.reflect.ClassTag

object RemoteTableList {

  def create[T](
    pageSize: Int = 50
  )(
    fetch: RemotePageQuery => Future[Table[T]]
  )(using executionContext: ExecutionContext): RemoteListProperty[T, RemotePageQuery] = {
    val normalizedPageSize = math.max(1, pageSize)

    ListProperty.remote[T, RemotePageQuery](
      loader = ListProperty.RemoteLoader { query =>
        fetch(query).map { table =>
          val loadedCount = table.rows.length
          val nextIndex = query.index + loadedCount
          val totalCount = math.max(table.size, nextIndex)

          ListProperty.RemotePage[T, RemotePageQuery](
            items = table.rows.toSeq,
            offset = Some(query.index),
            nextQuery =
              if (nextIndex < totalCount) Some(query.copy(nextIndex = nextIndex, nextLimit = normalizedPageSize))
              else None,
            totalCount = Some(totalCount),
            hasMore = Some(nextIndex < totalCount)
          )
        }.toJSPromise
      },
      initialQuery = RemotePageQuery.first(normalizedPageSize),
      executionContext = executionContext,
      sortUpdater = Some((query, sorting) =>
        query.copy(
          nextIndex = 0,
          nextLimit = normalizedPageSize,
          nextSorting = sorting.toVector
        )
      ),
      rangeQueryUpdater = Some((query, index, limit) =>
        query.copy(
          nextIndex = index,
          nextLimit = math.max(1, limit)
        )
      )
    )
  }

  def createMapped[A, B: ClassTag](
    pageSize: Int = 50
  )(
    fetch: RemotePageQuery => Future[Table[A]]
  )(
    mapRow: A => B
  )(using executionContext: ExecutionContext): RemoteListProperty[B, RemotePageQuery] =
    create[B](pageSize = pageSize) { query =>
      fetch(query).map { table =>
        new Table[B](
          rows = table.rows.iterator.map(mapRow).toJSArray,
          size = table.size
        )
      }
    }

  def reloadFirstPage[T](
    items: RemoteListProperty[T, RemotePageQuery],
    pageSize: Int = 50
  ): Unit =
    items.reload(RemotePageQuery.first(pageSize, sorting = items.getSorting)).toFuture.recover { case _ => () }
}
