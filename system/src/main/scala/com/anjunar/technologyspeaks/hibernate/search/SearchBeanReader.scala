package com.anjunar.technologyspeaks.hibernate.search

import com.anjunar.scala.universe.introspector.AnnotationIntrospector
import jakarta.json.bind.annotation.JsonbProperty
import com.anjunar.technologyspeaks.hibernate.search.annotations.{RestPredicate, RestSort}
import jakarta.persistence.criteria.{Expression, Order, Predicate}
import org.hibernate.Session
import org.hibernate.query.criteria.{HibernateCriteriaBuilder, JpaCriteriaQuery, JpaRoot}
import org.springframework.beans.factory.ObjectProvider

import scala.util.boundary
import scala.util.boundary.break

object SearchBeanReader {

  def read[E](
    searchBean: AbstractSearch,
    session: Session,
    builder: HibernateCriteriaBuilder,
    root: JpaRoot[E],
    query: JpaCriteriaQuery[?],
    instances: ObjectProvider[PredicateProvider[Any, E]]
  ): HibernateSearchContextResult = {
    val beanModel = AnnotationIntrospector.createWithType(searchBean.getClass, classOf[JsonbProperty])
    val predicates = new java.util.ArrayList[Predicate]()
    val selection = new java.util.ArrayList[Expression[?]]()
    val parameters = new java.util.HashMap[String, Any]()

    beanModel.properties.foreach { property =>
      val restPredicate = property.findAnnotation(classOf[RestPredicate])

      if (restPredicate != null) {
        val provider = findProvider(instances, restPredicate.value())
        val value =
          try {
            property.get(searchBean)
          } catch {
            case _: Exception => null
          }

        if (provider != null && value != null) {
          val name =
            if (restPredicate.name().isBlank) property.name
            else restPredicate.name()

          provider.build(
            Context(
              value,
              session,
              builder,
              predicates,
              root,
              query,
              selection,
              name,
              parameters
            )
          )
        }
      }
    }

    HibernateSearchContextResult(selection, predicates, parameters)
  }

  def order[E](
    searchBean: AbstractSearch,
    session: Session,
    builder: HibernateCriteriaBuilder,
    root: JpaRoot[E],
    query: JpaCriteriaQuery[?],
    predicates: java.util.List[Predicate],
    selection: java.util.List[Expression[?]],
    instances: ObjectProvider[SortProvider[Any, E]]
  ): java.util.List[Order] = {
    val beanModel = AnnotationIntrospector.createWithType(searchBean.getClass, classOf[JsonbProperty])
    boundary {
      for (property <- beanModel.properties) {
        val restSort = property.findAnnotation(classOf[RestSort])

        if (restSort != null) {
          val value = property.get(searchBean)
          if (value != null) {
            val sortProvider = findProvider(instances, restSort.value())
            if (sortProvider != null) {
              break(
                sortProvider.sort(
                  Context(
                    searchBean,
                    session,
                    builder,
                    predicates,
                    root,
                    query,
                    selection,
                    property.name,
                    new java.util.HashMap[String, Any]()
                  )
                )
              )
            }
          }
        }
      }

      new java.util.ArrayList[Order]()
    }
  }

  private def findProvider[T](instances: ObjectProvider[T], clazz: Class[?]): T = {
    val iterator = instances.iterator()
    while (iterator.hasNext) {
      val provider = iterator.next()
      if (provider.getClass == clazz) {
        return provider
      }
    }
    null.asInstanceOf[T]
  }

}
