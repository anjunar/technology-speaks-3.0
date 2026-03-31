package com.anjunar.scala.enterprise.macros.reflection

import com.anjunar.scala.enterprise.macros.ReflectionSupport
import com.anjunar.scala.enterprise.macros.reflection.{GenericArrayType, ParameterizedType, SimpleClass, Type => JType}

import scala.quoted.*

object TypeMacros {

  inline def toType[T]: JType =
    ${ toTypeImpl[T] }

  inline def toSimpleClass[T]: SimpleClass[T] =
    ${ toSimpleClassImpl[T] }

  private def toTypeImpl[T](using Type[T], Quotes): Expr[JType] = {
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]
    val typeExpr = runtimeTypeExpr(tpe)

    typeExpr
  }

  private def toSimpleClassImpl[T](using Type[T], Quotes): Expr[SimpleClass[T]] = {
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]
    val normalized = normalizeType(tpe)

    normalized match {
      case AppliedType(_, _) =>
        report.errorAndAbort(
          s"toSimpleClass kann nur für einfache Typen verwendet werden, nicht für parametrisierte Typen wie ${tpe.show}"
        )

      case _ =>
        val clsExpr = runtimeClassExpr(normalized)
        '{ $clsExpr.asInstanceOf[SimpleClass[T]] }
    }
  }

  private def normalizeType(using Quotes)(tpe: quotes.reflect.TypeRepr): quotes.reflect.TypeRepr = {
    import quotes.reflect.*
    def loop(current: TypeRepr): TypeRepr = {
      val simplified = current.widenTermRefByName.dealias.simplified

      simplified match {
        case AnnotatedType(inner, _) =>
          loop(inner)

        case ByNameType(inner) =>
          loop(inner)

        case mt: MethodType if mt.paramTypes.isEmpty =>
          loop(mt.resType)

        case pt: PolyType =>
          loop(pt.resType)

        case ct: ConstantType =>
          val widened = ct.widen
          if widened =:= ct then ct else loop(widened)

        case other =>
          other
      }
    }

    loop(tpe)
  }

  private def runtimeClassExpr(using Quotes)(tpe: quotes.reflect.TypeRepr): Expr[SimpleClass[?]] = {
    import quotes.reflect.*
    val normalized = normalizeType(tpe)

    normalized match {
      case tr: TypeRef if tr.typeSymbol.flags.is(Flags.Param) =>
        report.errorAndAbort(
          s"BUG: tried to resolve runtime class for type parameter '${tr.typeSymbol.name}' from type ${normalized.show(using Printer.TypeReprStructure)}"
        )

      case _ =>
        val sym = normalized.typeSymbol
        if sym == Symbol.noSymbol then
          report.errorAndAbort(
            s"No runtime class symbol available for type ${normalized.show(using Printer.TypeReprStructure)}"
          )

        '{ ReflectionSupport.resolveClass(${ Expr(sym.fullName) }) }
    }
  }

  private def runtimeTypeExpr(using Quotes)(tpe: quotes.reflect.TypeRepr): Expr[JType] = {
    import quotes.reflect.*
    normalizeType(tpe) match {
      case tr: TypeRef if tr.typeSymbol.flags.is(Flags.Param) =>
        val name = tr.typeSymbol.name
        '{ ReflectionSupport.typeVariable(${ Expr(name) }) }

      case AppliedType(rawType, args) =>
        val rawClassExpr = runtimeClassExpr(rawType)
        val argExprs: Seq[Expr[JType]] = args.map(runtimeTypeExpr)
        val argArrayExpr: Expr[Array[JType]] = '{ Array[JType](${ Varargs(argExprs) }*) }

        '{
          ReflectionSupport.parameterized(
            $rawClassExpr,
            $argArrayExpr
          )
        }

      case t if t <:< TypeRepr.of[Array[?]] =>
        t.asType match {
          case '[Array[a]] =>
            val componentExpr = runtimeTypeExpr(TypeRepr.of[a])
            '{
              ReflectionSupport.genericArray($componentExpr)
            }
        }

      case other =>
        val clsExpr = runtimeClassExpr(other)
        '{ $clsExpr: JType }
    }
  }

}
