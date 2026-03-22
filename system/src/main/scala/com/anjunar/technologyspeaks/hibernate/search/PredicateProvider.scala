package com.anjunar.technologyspeaks.hibernate.search

trait PredicateProvider[V, E] {

  def build(context: Context[V, E]): Unit

}
