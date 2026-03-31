package jfx.core.meta

import scala.quoted.*

object JsonTypeMacro {

  /**
   * Liest den @JsonType Wert einer Klasse zur Compilezeit aus.
   * Falls keine @JsonType Annotation vorhanden ist, wird der SimpleName zurückgegeben.
   */
  inline def getJsonTypeName[E]: String = ${ getJsonTypeNameImpl[E] }

  private def getJsonTypeNameImpl[E: Type](using Quotes): Expr[String] = {
    import quotes.reflect.*

    val classSymbol = TypeRepr.of[E].typeSymbol
    
    val jsonTypeAnnotation = classSymbol.annotations.find { ann =>
      ann.tpe.typeSymbol.fullName == "jfx.json.JsonType"
    }

    val typeName = jsonTypeAnnotation match {
      case Some(ann) =>
        ann match {
          case Apply(_, List(Literal(StringConstant(value)))) =>
            Expr(value)
          case _ =>
            Expr(classSymbol.name)
        }
      case None =>
        Expr(classSymbol.name)
    }

    typeName
  }

}
