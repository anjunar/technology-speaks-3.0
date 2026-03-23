package com.anjunar.technologyspeaks.followers

import com.anjunar.technologyspeaks.core.User
import com.anjunar.technologyspeaks.hibernate.search.annotations.RestPredicate
import com.anjunar.technologyspeaks.hibernate.search.{AbstractSearch, Context, PredicateProvider}

import scala.beans.BeanProperty
import scala.compiletime.uninitialized

class RelationShipSearch(sort: java.util.List[String],
                         index: Int,
                         limit: Int) 
  extends AbstractSearch(sort, index, limit) {

  @RestPredicate(classOf[RelationShipSearch.UserPredicate])
  var user: User = uninitialized

}

object RelationShipSearch {

  class UserPredicate extends PredicateProvider[User, RelationShip] {
    override def build(context: Context[User, RelationShip]): Unit = {
      context.predicates.add(
        context.builder.equal(
          context.root.get[RelationShip]("user").get[User]("id"),
          context.value.id
        )
      )
    }
  }

}
