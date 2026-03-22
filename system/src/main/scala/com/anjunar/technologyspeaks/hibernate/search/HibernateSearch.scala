package com.anjunar.technologyspeaks.hibernate.search

import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.{Expression, Order, Predicate}
import org.hibernate.Session
import org.hibernate.query.criteria.{HibernateCriteriaBuilder, JpaCriteriaQuery, JpaRoot}
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

@Component
class HibernateSearch(
  val entityManager: EntityManager,
  val predicateProvider: ObjectProvider[? <: PredicateProvider[?, ?]],
  val sortProvider: ObjectProvider[? <: SortProvider[?, ?]]
) {

  def searchContext[S <: AbstractSearch](search: S): HibernateSearchContext =
    new HibernateSearchContext {
      override def apply[C](
        session: Session,
        builder: HibernateCriteriaBuilder,
        query: JpaCriteriaQuery[?],
        root: JpaRoot[C]
      ): HibernateSearchContextResult =
        SearchBeanReader.read(search, session, builder, root, query, predicateProvider.asInstanceOf[ObjectProvider[PredicateProvider[Any, C]]])

      override def sort[C](
        session: Session,
        builder: HibernateCriteriaBuilder,
        query: JpaCriteriaQuery[?],
        root: JpaRoot[C],
        predicates: java.util.List[Predicate],
        selection: java.util.List[Expression[?]]
      ): java.util.List[Order] =
        SearchBeanReader.order(search, session, builder, root, query, predicates, selection, sortProvider.asInstanceOf[ObjectProvider[SortProvider[Any, C]]])
    }

  def entities[E, P](
    index: Int,
    limit: Int,
    entityClass: Class[E],
    projection: Class[P],
    context: HibernateSearchContext,
    select: (JpaCriteriaQuery[P], JpaRoot[E], java.util.List[Expression[?]], HibernateCriteriaBuilder) => JpaCriteriaQuery[P]
  ): java.util.List[P] = {
    val session = entityManager.unwrap(classOf[Session])
    val builder = session.getCriteriaBuilder
    val query = builder.createQuery(projection)
    val from = query.from(entityClass)

    val result = context.apply(session, builder, query, from)
    val order = context.sort(session, builder, query, from, result.predicates, result.selection)

    select(query, from, result.selection, builder).where(result.predicates).orderBy(order)

    val typedQuery = session.createQuery(query)
      .setFirstResult(index)
      .setMaxResults(limit)

    result.parameters.forEach((key, value) => typedQuery.setParameter(key, value))
    typedQuery.getResultList
  }

  def count[E](
    entityClass: Class[E],
    context: HibernateSearchContext
  ): Long = {
    val session = entityManager.unwrap(classOf[Session])
    val builder = session.getCriteriaBuilder
    val query = builder.createQuery(classOf[java.lang.Long])
    val from = query.from(entityClass)

    val result = context.apply(session, builder, query, from)
    query.select(builder.count()).where(result.predicates)

    val typedQuery = session.createQuery(query)
    result.parameters.forEach((key, value) => typedQuery.setParameter(key, value))
    typedQuery.getSingleResult
  }

}
