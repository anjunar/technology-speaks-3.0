package com.anjunar.technologyspeaks.documents

import com.anjunar.technologyspeaks.hibernate.search.annotations.RestPredicate
import com.anjunar.technologyspeaks.hibernate.search.{AbstractSearch, Context, PredicateProvider}
import jakarta.json.bind.annotation.JsonbProperty
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestParam

import scala.beans.BeanProperty

class DocumentSearch(
  nameValue: String = null,
  sort: java.util.List[String] = new java.util.ArrayList[String](),
  index: Int = 0,
  limit: Int = 5
) extends AbstractSearch(sort, index, limit) {

  @JsonbProperty
  @RequestParam
  @RestPredicate(classOf[DocumentSearch.DocumentPredicate])
  @BeanProperty
  val name: String = nameValue

}

object DocumentSearch {

  @Component
  class DocumentPredicate extends PredicateProvider[String, Document] {

    override def build(context: Context[String, Document]): Unit = {
      val rawValue = context.value
      val value = rawValue.trim
      if (value.isEmpty) {
        return
      }

      val parameterName = s"${context.name}_query"
      val parameter = context.builder.parameter(classOf[String], parameterName)
      context.parameters.put(parameterName, value)

      val similarity = context.builder.function(
        "similarity",
        classOf[java.lang.Double],
        context.builder.lower(context.root.get("title")),
        context.builder.lower(parameter)
      )

      context.selection.add(similarity)
      context.predicates.add(
        context.builder.greaterThanOrEqualTo[java.lang.Double](similarity, java.lang.Double.valueOf(0.1d))
      )
    }

  }

}
