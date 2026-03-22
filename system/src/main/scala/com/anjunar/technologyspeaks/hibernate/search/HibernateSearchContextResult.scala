package com.anjunar.technologyspeaks.hibernate.search

import jakarta.persistence.criteria.{Expression, Predicate}

case class HibernateSearchContextResult(
  selection: java.util.List[Expression[?]],
  predicates: java.util.List[Predicate],
  parameters: java.util.Map[String, Any]
)
