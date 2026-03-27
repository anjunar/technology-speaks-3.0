package com.anjunar.technologyspeaks.core

import com.anjunar.technologyspeaks.hibernate.search.AbstractSearch
import com.anjunar.technologyspeaks.hibernate.search.annotations.RestPredicate
import com.anjunar.technologyspeaks.hibernate.search.{Context, PredicateProvider}
import com.anjunar.technologyspeaks.security.IdentityHolder
import jakarta.json.bind.annotation.JsonbProperty
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.JoinType
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.stereotype.Component

import scala.annotation.meta.field
import scala.beans.BeanProperty

class UserSearch(
  @(JsonbProperty @field)
  @(RestPredicate @field)(classOf[UserSearch.UserPredicate])
  val name: String,
  sort: java.util.List[String] = new java.util.ArrayList[String](),
  index: Int = 0,
  limit: Int = 5
) extends AbstractSearch(sort, index, limit) {


}

object UserSearch {

  @Component
  class UserPredicate(val identityHolder: IdentityHolder) extends PredicateProvider[String, User] {

    override def build(context: Context[String, User]): Unit = {
      val rawValue = Option(context.value).map(_.trim).getOrElse("")
      if (rawValue.isEmpty) {
        return
      }

      val parameterName = s"${context.name}_query"
      val parameter = context.builder.parameter(classOf[String], parameterName)
      context.parameters.put(parameterName, s"%${rawValue.toLowerCase}%")

      val disjunction = new java.util.ArrayList[jakarta.persistence.criteria.Predicate]()
      disjunction.add(
        context.builder.like(
          context.builder.lower(context.root.get("nickName")),
          parameter
        )
      )

      val infoJoin = context.root.join[User, UserInfo]("info", JoinType.LEFT)
      disjunction.add(
        context.builder.and(Array[Predicate](
          canViewManagedProperty(context, "firstName"),
          context.builder.like(
            context.builder.lower(infoJoin.get("firstName")),
            parameter
          )
        )*)
      )
      disjunction.add(
        context.builder.and(Array[Predicate](
          canViewManagedProperty(context, "lastName"),
          context.builder.like(
            context.builder.lower(infoJoin.get("lastName")),
            parameter
          )
        )*)
      )

      context.predicates.add(context.builder.or(disjunction.toArray(new Array[Predicate](disjunction.size()))*))
    }

    private def canViewManagedProperty(context: Context[String, User], propertyName: String) = {
      val ownerVisible =
        if (identityHolder.user == null || identityHolder.user.id == null) context.builder.disjunction()
        else context.builder.equal(context.root.get("id"), identityHolder.user.id)

      val visibleForAll = context.query.subquery(classOf[java.lang.Long])
      val visibleForAllRoot = visibleForAll.from(classOf[ManagedProperty])
      visibleForAll.select(context.builder.literal(1L))
      visibleForAll.where(
        context.builder.equal(
          visibleForAllRoot.get[EntityView]("view").get[User]("user").get("id"),
          context.root.get("id")
        ),
        context.builder.equal(visibleForAllRoot.get[String]("name"), propertyName),
        context.builder.isTrue(visibleForAllRoot.get[java.lang.Boolean]("visibleForAll"))
      )

      val visibleForUser =
        if (identityHolder.user == null || identityHolder.user.id == null) context.builder.disjunction()
        else {
          val subquery = context.query.subquery(classOf[java.lang.Long])
          val managedPropertyRoot = subquery.from(classOf[ManagedProperty])
          val users = managedPropertyRoot.join[ManagedProperty, User]("users", JoinType.LEFT)
          subquery.select(context.builder.literal(1L))
          subquery.where(
            context.builder.equal(
              managedPropertyRoot.get[EntityView]("view").get[User]("user").get("id"),
              context.root.get("id")
            ),
            context.builder.equal(managedPropertyRoot.get[String]("name"), propertyName),
            context.builder.equal(users.get("id"), identityHolder.user.id)
          )
          context.builder.exists(subquery)
        }

      context.builder.or(ownerVisible, context.builder.exists(visibleForAll), visibleForUser)
    }
  }

}
