package com.anjunar.technologyspeaks.followers

import com.anjunar.technologyspeaks.core.{User, UserInfo}
import com.anjunar.technologyspeaks.hibernate.search.annotations.RestPredicate
import com.anjunar.technologyspeaks.hibernate.search.{AbstractSearch, Context, PredicateProvider}
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.criteria.{Expression, JoinType, Predicate}
import org.springframework.stereotype.Component

import scala.annotation.meta.field
import scala.compiletime.uninitialized
import scala.jdk.CollectionConverters.*

class RelationShipSearch(
  @(JsonbProperty @field)
  @(RestPredicate @field)(classOf[RelationShipSearch.NamePredicate])
  val name: String,
  @(JsonbProperty @field)
  @(RestPredicate @field)(classOf[RelationShipSearch.GroupsPredicate])
  val groups: java.util.List[Group],
  sort: java.util.List[String] = new java.util.ArrayList[String](),
  index: Int = 0,
  limit: Int = 5
)
  extends AbstractSearch(sort, index, limit) {

  @(JsonbProperty @field)
  @(RestPredicate @field)(classOf[RelationShipSearch.UserPredicate])
  var user: User = uninitialized

}

object RelationShipSearch {

  @Component
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

  @Component
  class NamePredicate extends PredicateProvider[String, RelationShip] {

    override def build(context: Context[String, RelationShip]): Unit = {
      val rawValue = Option(context.value).map(_.trim).getOrElse("")
      if (rawValue.isEmpty) {
        return
      }

      val parameterName = s"${context.name}_query"
      val parameter = context.builder.parameter(classOf[String], parameterName)
      context.parameters.put(parameterName, rawValue.toLowerCase)

      val followerJoin = context.root.join[RelationShip, User]("follower", JoinType.INNER)
      val disjunction = new java.util.ArrayList[Predicate]()

      disjunction.add(
        similarityPredicate(
          context,
          context.builder.lower(followerJoin.get("nickName")),
          parameter
        )
      )

      val infoJoin = followerJoin.join[User, UserInfo]("info", JoinType.LEFT)
      disjunction.add(
        similarityPredicate(
          context,
          context.builder.lower(infoJoin.get("firstName")),
          parameter
        )
      )
      disjunction.add(
        similarityPredicate(
          context,
          context.builder.lower(infoJoin.get("lastName")),
          parameter
        )
      )

      context.predicates.add(context.builder.or(disjunction.toArray(new Array[Predicate](disjunction.size()))*))
    }

    private def similarityPredicate(
      context: Context[String, RelationShip],
      expression: Expression[String],
      parameter: jakarta.persistence.criteria.ParameterExpression[String]
    ): Predicate = {
      val similarity = context.builder.function(
        "similarity",
        classOf[java.lang.Double],
        expression,
        parameter
      )

      context.selection.add(similarity)

      context.builder.greaterThanOrEqualTo[java.lang.Double](similarity, java.lang.Double.valueOf(0.1d))
    }
  }

  @Component
  class GroupsPredicate extends PredicateProvider[java.util.List[Group], RelationShip] {

    override def build(context: Context[java.util.List[Group], RelationShip]): Unit = {
      val groupIds =
        Option(context.value)
          .map(_.asScala.iterator.flatMap(group => Option(group.id)).toSeq.distinct)
          .getOrElse(Seq.empty)

      if (groupIds.isEmpty) {
        return
      }

      val groupsJoin = context.root.join[RelationShip, Group]("groups", JoinType.INNER)
      context.predicates.add(groupsJoin.get("id").in(groupIds.asJava))
      context.query.distinct(true)
    }
  }

}
