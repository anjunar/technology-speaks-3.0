package com.anjunar.scala.enterprise.macros

import scala.quoted.*
import com.anjunar.scala.enterprise.macros.reflection.SimpleClass

/**
 * Macro zum Auslesen von Klassenannotationen und Properties zur Compilezeit.
 */
object ClassAnnotationMacros {

  /**
   * Liest alle Annotationen einer Klasse zur Compilezeit aus.
   * 
   * @tparam E Der Typ der Klasse, deren Annotationen ausgelesen werden sollen
   * @return Ein Array von Annotation-Objekten
   */
  inline def describeClassAnnotations[E]: Array[Annotation] =
    ${ describeClassAnnotationsImpl[E] }

  /**
   * Erstellt eine vollständige Klassenbeschreibung mit Annotationen und Properties.
   * 
   * @tparam E Der Typ der Klasse
   * @return Ein SimpleClass-Objekt mit allen Metadaten
   */
  inline def describeClass[E]: SimpleClass[E] =
    ${ describeClassImpl[E] }

  private def describeClassImpl[E: Type](using Quotes): Expr[SimpleClass[E]] = {
    import quotes.reflect.*

    val classSymbol = TypeRepr.of[E].typeSymbol
    val fullNameExpr = Expr(classSymbol.fullName)
    val simpleNameExpr = Expr(classSymbol.name)
    val annotationsExpr = extractAnnotations(classSymbol)

    // Properties aus der Klasse und allen Basisklassen auslesen
    val propertiesExpr = PropertyMacros.describePropertiesImpl[E]
    
    // Basisklassen/Traits zur Compilezeit auslesen
    val baseTypesExpr = extractBaseTypes[E]

    '{
      SimpleClass[E](
        typeName = $fullNameExpr,
        annotations = $annotationsExpr,
        properties = $propertiesExpr.toArray,
        baseTypes = $baseTypesExpr
      )
    }
  }
  
  private def extractBaseTypes[E: Type](using Quotes): Expr[Array[String]] = {
    import quotes.reflect.*
    
    val baseTypes = TypeRepr.of[E].baseClasses.map(_.fullName).filter(_.nonEmpty)
    '{ Array(${ Varargs(baseTypes.map(Expr(_))) }*) }
  }

  private def describeClassAnnotationsImpl[E: Type](using Quotes): Expr[Array[Annotation]] = {
    import quotes.reflect.*

    val classSymbol = TypeRepr.of[E].typeSymbol
    
    extractAnnotations(classSymbol)
  }

  private def extractAnnotations(using Quotes)(symbol: quotes.reflect.Symbol): Expr[Array[Annotation]] = {
    import quotes.reflect.*

    def extractParamsFromAnnotation(ann: Term): List[(String, Expr[Any])] = {
      ann match {
        case Apply(_, args) =>
          val paramNames = ann.tpe.typeSymbol.primaryConstructor.paramSymss.flatten.map(_.name)
          args.zip(paramNames).collect {
            case (NamedArg(name, Literal(StringConstant(v))), _) =>
              (name, Expr(v).asExprOf[Any])
            case (NamedArg(name, Literal(IntConstant(v))), _) =>
              (name, Expr(v).asExprOf[Any])
            case (NamedArg(name, Literal(LongConstant(v))), _) =>
              (name, Expr(v).asExprOf[Any])
            case (NamedArg(name, Literal(DoubleConstant(v))), _) =>
              (name, Expr(v).asExprOf[Any])
            case (NamedArg(name, Literal(FloatConstant(v))), _) =>
              (name, Expr(v).asExprOf[Any])
            case (NamedArg(name, Literal(BooleanConstant(v))), _) =>
              (name, Expr(v).asExprOf[Any])
            case (Literal(StringConstant(v)), name) =>
              (name, Expr(v).asExprOf[Any])
            case (Literal(IntConstant(v)), name) =>
              (name, Expr(v).asExprOf[Any])
            case (Literal(LongConstant(v)), name) =>
              (name, Expr(v).asExprOf[Any])
            case (Literal(DoubleConstant(v)), name) =>
              (name, Expr(v).asExprOf[Any])
            case (Literal(FloatConstant(v)), name) =>
              (name, Expr(v).asExprOf[Any])
            case (Literal(BooleanConstant(v)), name) =>
              (name, Expr(v).asExprOf[Any])
          }
        case _ =>
          Nil
      }
    }

    val annotationExprs = symbol.annotations.flatMap { ann =>
      val className = ann.tpe.typeSymbol.fullName
      val params = extractParamsFromAnnotation(ann)

      if params.isEmpty then
        Some('{ Annotation(${ Expr(className) }, Map.empty) })
      else
        val paramsExpr = '{ Map(${ Varargs(params.map { case (k, v) => '{ (${ Expr(k) }, $v) } }) }*) }
        Some('{ Annotation(${ Expr(className) }, $paramsExpr) })
    }

    '{ Array(${ Varargs(annotationExprs) }*) }
  }

}
