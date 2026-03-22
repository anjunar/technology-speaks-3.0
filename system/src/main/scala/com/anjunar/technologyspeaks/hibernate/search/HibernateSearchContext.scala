package com.anjunar.technologyspeaks.hibernate.search

import jakarta.persistence.criteria.{Expression, Order, Predicate}
import org.hibernate.Session
import org.hibernate.query.criteria.{HibernateCriteriaBuilder, JpaCriteriaQuery, JpaRoot}

trait HibernateSearchContext {

  def apply[C](
    session: Session,
    builder: HibernateCriteriaBuilder,
    query: JpaCriteriaQuery[?],
    root: JpaRoot[C]
  ): HibernateSearchContextResult

  def sort[C](
    session: Session,
    builder: HibernateCriteriaBuilder,
    query: JpaCriteriaQuery[?],
    root: JpaRoot[C],
    predicates: java.util.List[Predicate],
    selection: java.util.List[Expression[?]]
  ): java.util.List[Order]

}
