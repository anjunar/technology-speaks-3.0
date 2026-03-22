package com.anjunar.technologyspeaks.hibernate

import com.anjunar.technologyspeaks.SpringContext
import jakarta.persistence.{Entity, EntityGraph, EntityManager, NoResultException}
import org.slf4j.{Logger, LoggerFactory}

import java.lang.reflect.ParameterizedType

abstract class RepositoryContext[E] {

  val log: Logger = LoggerFactory.getLogger(classOf[RepositoryContext[?]])

  lazy val clazz: Class[E] = {
    val superClass = getClass.getGenericSuperclass
    require(superClass.isInstanceOf[ParameterizedType], "RepositoryContext must be subclassed with generic type")
    superClass.asInstanceOf[ParameterizedType].getActualTypeArguments.apply(0).asInstanceOf[Class[E]]
  }

  def entityManager(): EntityManager = SpringContext.entityManager()

  def find(id: Any): E =
    entityManager().find(clazz, id)

  def find(graph: EntityGraph[E], id: Any): E = {
    val entityManager = this.entityManager()
    val hints = java.util.Map.of[String, Any]("jakarta.persistence.loadgraph", graph)
    entityManager.find(clazz, id, hints)
  }

  def findAll(): java.util.List[E] = {
    val entityManager = this.entityManager()
    val criteriaBuilder = entityManager.getCriteriaBuilder
    val query = criteriaBuilder.createQuery(clazz)
    val root = query.from(clazz)
    query.select(root)
    entityManager.createQuery(query).getResultList
  }

  def query(parameters: (String, Any)*): E = {
    val entityManager = this.entityManager()

    val entityAnnotation = clazz.getAnnotation(classOf[Entity])
    var entityName =
      if (entityAnnotation == null) "" else entityAnnotation.name()

    if (entityName.isEmpty) {
      entityName = clazz.getSimpleName
    }

    val sqlParams = parameters.map(parameter => s"e.${parameter._1} = :${parameter._1}").mkString(" and ")
    val jpql = s"select e from $entityName e where $sqlParams"
    val typedQuery = entityManager.createQuery(jpql, clazz)

    parameters.foreach { case (key, value) =>
      typedQuery.setParameter(key, value)
    }

    try {
      typedQuery.getSingleResult
    } catch {
      case _: NoResultException => null.asInstanceOf[E]
    }
  }

}
