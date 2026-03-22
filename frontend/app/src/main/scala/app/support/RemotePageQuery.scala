package app.support

class RemotePageQuery(
  var index: Int = 0,
  var limit: Int = 50
) {

  def copy(nextIndex: Int = index, nextLimit: Int = limit): RemotePageQuery =
    new RemotePageQuery(nextIndex, nextLimit)

  override def equals(other: Any): Boolean =
    other match {
      case query: RemotePageQuery =>
        query.index == index && query.limit == limit
      case _ =>
        false
    }

  override def hashCode(): Int =
    31 * index + limit

  override def toString: String =
    s"RemotePageQuery(index=$index, limit=$limit)"
}

object RemotePageQuery {

  def first(limit: Int): RemotePageQuery =
    new RemotePageQuery(index = 0, limit = math.max(1, limit))
}
