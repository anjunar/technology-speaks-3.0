package com.anjunar.technologyspeaks.hibernate.search

import jakarta.persistence.criteria.Order

trait SortProvider[V, E] {

  def sort(context: Context[V, E]): java.util.List[Order]

}
