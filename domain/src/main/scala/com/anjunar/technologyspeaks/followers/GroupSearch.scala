package com.anjunar.technologyspeaks.followers

import com.anjunar.technologyspeaks.core.User
import com.anjunar.technologyspeaks.hibernate.search.annotations.RestPredicate
import com.anjunar.technologyspeaks.hibernate.search.{AbstractSearch, Context, PredicateProvider}

import scala.beans.BeanProperty

class GroupSearch(
  sort: java.util.List[String],
  index: Int,
  limit: Int
) extends AbstractSearch(sort, index, limit) {

  @RestPredicate(classOf[GroupSearch.UserPredicate])
    var user: User = null

}

object GroupSearch {

  class UserPredicate extends PredicateProvider[User, Group] {
    override def build(context: Context[User, Group]): Unit = {
      context.predicates.add(
        context.builder.equal(
          context.root.get[Group]("user").get[User]("id"),
          context.value.id
        )
      )
    }
  }

}
