package com.anjunar.technologyspeaks.timeline

import com.anjunar.technologyspeaks.hibernate.search.AbstractSearch

class PostSearch(
  sort: java.util.List[String] = new java.util.ArrayList[String](),
  index: Int = 0,
  limit: Int = 5
) extends AbstractSearch(sort, index, limit)
