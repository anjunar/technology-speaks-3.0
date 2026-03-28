package app.support

import jfx.core.state.ListProperty

class RemotePageQuery(
  var index: Int = 0,
  var limit: Int = 50,
  var sorting: Vector[ListProperty.RemoteSort] = Vector.empty
) {

  def copy(
    nextIndex: Int = index,
    nextLimit: Int = limit,
    nextSorting: Vector[ListProperty.RemoteSort] = sorting
  ): RemotePageQuery =
    new RemotePageQuery(nextIndex, nextLimit, nextSorting)

  def sortSpecs: Seq[String] =
    sorting.map(sort => s"${sort.field}:${sort.direction}")

  def effectiveSortSpecs(default: Seq[String]): Seq[String] =
    if (sorting.nonEmpty) sortSpecs else default

  override def equals(other: Any): Boolean =
    other match {
      case query: RemotePageQuery =>
        query.index == index && query.limit == limit && query.sorting == sorting
      case _ =>
        false
    }

  override def hashCode(): Int =
    31 * (31 * index + limit) + sorting.hashCode()

  override def toString: String =
    s"RemotePageQuery(index=$index, limit=$limit, sorting=$sorting)"
}

object RemotePageQuery {

  def first(limit: Int, sorting: Vector[ListProperty.RemoteSort] = Vector.empty): RemotePageQuery =
    new RemotePageQuery(index = 0, limit = math.max(1, limit), sorting = sorting)
}
