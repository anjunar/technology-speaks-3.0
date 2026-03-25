package com.anjunar.json.mapper.schema

import scala.quoted.*

object PropertyMacros {

  inline def makeProperty[T, V](
                                 inline selector: T => V,
                                 rule: VisibilityRule[T]
                               ): Property[T, V] =
    ${ makePropertyImpl[T, V]('selector, 'rule) }

  private def makePropertyImpl[T: Type, V: Type](
                                                  selectorExpr: Expr[T => V],
                                                  ruleExpr: Expr[VisibilityRule[T]]
                                                )(using q: Quotes): Expr[Property[T, V]] = {
    import q.reflect.*

    def runtimeClassExpr(tpe: TypeRepr): Expr[Class[?]] = {
      val sym = tpe.dealias.simplified.typeSymbol
      if sym == Symbol.noSymbol then
        report.errorAndAbort(s"Kein Runtime-Class-Symbol für Typ: ${tpe.show}")
      val fullName: String = sym.fullName
      '{ RuntimeClassResolver.resolve(${Expr(fullName)}) }
    }

    def extractRuntimeClasses(tpe: TypeRepr): (Expr[Class[?]], Expr[Class[?]]) =
      tpe.dealias.simplified match {
        case AppliedType(collectionRaw, List(elementType))
          if collectionRaw <:< TypeRepr.of[java.util.Collection[?]] =>
          (runtimeClassExpr(collectionRaw), runtimeClassExpr(elementType))
        case other =>
          (runtimeClassExpr(other), '{ null.asInstanceOf[Class[?]] })
      }

    def fieldNameFromBody(body: Term, paramSym: Symbol): String =
      body match {
        case Inlined(_, _, inner) =>
          fieldNameFromBody(inner, paramSym)

        case Typed(inner, _) =>
          fieldNameFromBody(inner, paramSym)

        case Select(qualifier, fieldName) if qualifier.symbol == paramSym =>
          fieldName

        case _ =>
          report.errorAndAbort(
            s"Erwartet wurde ein einfacher Feldzugriff wie _.nickName, gefunden:\n${body.show(using Printer.TreeStructure)}"
          )
      }

    def propertyNameFromSelector(expr: Expr[T => V]): String = {
      def loop(term: Term, localDefs: Map[String, DefDef]): String =
        term match {
          case Inlined(_, _, inner) =>
            loop(inner, localDefs)

          case Block(stats, expr) =>
            val defs = stats.collect { case dd: DefDef => dd.name -> dd }.toMap
            loop(expr, localDefs ++ defs)

          case Lambda(List(param), body) =>
            fieldNameFromBody(body, param.symbol)

          case Closure(Ident(name), _) =>
            localDefs.get(name) match {
              case Some(dd) =>
                dd.rhs match {
                  case Some(rhs) =>
                    val params = dd.termParamss.flatMap(_.params)

                    params match {
                      case param :: Nil =>
                        fieldNameFromBody(rhs, param.symbol)

                      case _ =>
                        report.errorAndAbort(
                          s"Die Closure $name hat nicht genau einen Parameter:\n${dd.show(using Printer.TreeStructure)}"
                        )
                    }

                  case None =>
                    report.errorAndAbort(
                      s"Die Closure $name hat keinen Body:\n${dd.show(using Printer.TreeStructure)}"
                    )
                }

              case None =>
                report.errorAndAbort(
                  s"Closure $name konnte nicht im umgebenden Block aufgelöst werden.\n${term.show(using Printer.TreeStructure)}"
                )
            }
            
          case _ =>
            report.errorAndAbort(
              s"Erwartet wurde ein einfacher Selektor wie _.nickName, gefunden:\n${term.show(using Printer.TreeStructure)}"
            )
        }

      loop(expr.asTerm, Map.empty)
    }

    val propertyName = propertyNameFromSelector(selectorExpr)
    val (propertyTypeExpr, collectionTypeExpr) = extractRuntimeClasses(TypeRepr.of[V])

    '{
      new Property[T, V](
        ${Expr(propertyName)},
        $propertyTypeExpr,
        $collectionTypeExpr,
        $ruleExpr
      )
    }
  }
}