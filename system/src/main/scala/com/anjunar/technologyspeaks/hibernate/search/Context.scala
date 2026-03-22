package com.anjunar.technologyspeaks.hibernate.search

import jakarta.persistence.criteria.{Expression, Predicate}
import org.hibernate.Session
import org.hibernate.query.criteria.{HibernateCriteriaBuilder, JpaCriteriaQuery, JpaRoot}

case class Context[V, E](
  value: V,
  session: Session,
  builder: HibernateCriteriaBuilder,
  predicates: java.util.List[Predicate],
  root: JpaRoot[E],
  query: JpaCriteriaQuery[?],
  selection: java.util.List[Expression[?]],
  name: String,
  parameters: java.util.Map[String, Any]
)
