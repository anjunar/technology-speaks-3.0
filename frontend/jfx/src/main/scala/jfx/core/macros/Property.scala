package jfx.core.macros

import jfx.core.state.{ListProperty, Property, PropertyAccess}

import scala.reflect.ClassTag
import scala.quoted.*
import scala.scalajs.js

inline def property[T, V](inline selector: T => V)(using ClassTag[V]): PropertyAccess[T, V] =
  ${ propertyImpl[T, V]('selector, '{ summon[ClassTag[V]] }) }

inline def typedProperty[T, V, Value](inline selector: T => V)(using ClassTag[V], ClassTag[Value]): PropertyAccess[T, V] =
  ${ typedPropertyImpl[T, V, Value]('selector, '{ summon[ClassTag[V]] }, '{ summon[ClassTag[Value]] }) }

private def propertyImpl[T: Type, V: Type](
  selector: Expr[T => V],
  classTagExpr: Expr[ClassTag[V]]
)(using Quotes): Expr[PropertyAccess[T, V]] = {
  import quotes.reflect.*

  def inferWrappedValueTypeExpr: Expr[Class[?] | Null] =
    TypeRepr.of[V].dealias.simplified match {
      case AppliedType(wrapperType, List(innerType))
          if wrapperType =:= TypeRepr.of[Property[?]] || wrapperType =:= TypeRepr.of[ListProperty[?]] =>
        innerType.asType match
          case '[inner] =>
            Expr.summon[ClassTag[inner]] match
              case Some(innerClassTag) => '{ $innerClassTag.runtimeClass }
              case None                => '{ null }
      case _ =>
        '{ null }
    }

  selector.asTerm match {
    case Inlined(_, _, Lambda(_, Select(_, fieldName))) =>
      '{
        new PropertyAccess[T, V] {
          val name: String = ${ Expr(fieldName) }
          val classTag: ClassTag[V] = $classTagExpr
          override val valueType: Class[?] | Null = ${ inferWrappedValueTypeExpr }

          def get(obj: T): Option[V] =
            Some($selector(obj))

          def set(obj: T, value: V): Unit =
            ${
              val objTerm = '{ obj }.asTerm
              val valueTerm = '{ value }.asTerm

              val tpeSym = TypeRepr.of[T].typeSymbol
              val fieldSym = tpeSym.memberField(fieldName)
              val setterSyms = tpeSym.memberMethod(s"${fieldName}_=")

              if fieldSym != Symbol.noSymbol && fieldSym.flags.is(Flags.Mutable) then
                Assign(Select(objTerm, fieldSym), valueTerm).asExprOf[Unit]
              else if setterSyms.nonEmpty then
                Apply(Select(objTerm, setterSyms.head), List(valueTerm)).asExprOf[Unit]
              else
                report.errorAndAbort(s"Selector '$fieldName' is not writable on ${TypeRepr.of[T].show}")
            }
        }
      }

    case _ =>
      report.error("Selector must be a simple field access like _.fieldName")
      '{???}
  }
}

private def typedPropertyImpl[T: Type, V: Type, Value: Type](
  selector: Expr[T => V],
  classTagExpr: Expr[ClassTag[V]],
  valueClassTagExpr: Expr[ClassTag[Value]]
)(using Quotes): Expr[PropertyAccess[T, V]] = {
  import quotes.reflect.*

  def assignFieldOption(fieldName: String, objTerm: Term, rhs: Term): Option[Term] = {
    val tpeSym = TypeRepr.of[T].typeSymbol
    val fieldSym = tpeSym.memberField(fieldName)
    val setterSyms = tpeSym.memberMethod(s"${fieldName}_=")

    if fieldSym != Symbol.noSymbol && fieldSym.flags.is(Flags.Mutable) then
      Some(Assign(Select(objTerm, fieldSym), rhs))
    else if setterSyms.nonEmpty then
      Some(Apply(Select(objTerm, setterSyms.head), List(rhs)))
    else
      None
  }

  def assignFieldOrAbort(fieldName: String, objTerm: Term, rhs: Term): Term =
    assignFieldOption(fieldName, objTerm, rhs).getOrElse {
      report.errorAndAbort(s"Selector '$fieldName' is not writable on ${TypeRepr.of[T].show}")
    }

  def missingWrapperExpr(fieldName: String): Expr[Unit] =
    '{
      throw new IllegalStateException(
        ${ Expr(s"typedProperty selector '$fieldName' points to an uninitialized wrapper on ${Type.show[T]}. Initialize the val with Property(...) or ListProperty(...).") }
      )
    }

  def assignOrFailExpr(fieldName: String, objTerm: Term, rhs: Term): Expr[Unit] =
    assignFieldOption(fieldName, objTerm, rhs) match {
      case Some(assignTerm) => assignTerm.asExprOf[Unit]
      case None             => missingWrapperExpr(fieldName)
    }

  def directAssignExpr(fieldName: String, objTerm: Term, rhs: Term): Expr[Unit] =
    assignFieldOrAbort(fieldName, objTerm, rhs).asExprOf[Unit]

  selector.asTerm match {
    case Inlined(_, _, Lambda(_, Select(_, fieldName))) =>
      val selectedType = TypeRepr.of[V].dealias.simplified

      '{
        new PropertyAccess[T, V] {
          val name: String = ${ Expr(fieldName) }
          val classTag: ClassTag[V] = $classTagExpr
          override val valueType: Class[?] | Null = $valueClassTagExpr.runtimeClass

          def get(obj: T): Option[V] =
            Some($selector(obj))

          def set(obj: T, value: V): Unit =
            ${
              val objTerm = '{ obj }.asTerm
              val valueExpr = '{ value.asInstanceOf[Any] }

              if selectedType <:< TypeRepr.of[Property[?]] then
                selectedType.asType match
                  case '[Property[inner]] =>
                    '{
                      val current = ${ Select.unique(objTerm, fieldName).asExprOf[Property[inner] | Null] }
                      val decoded = $valueExpr.asInstanceOf[inner]

                      if (current == null) {
                        ${
                          assignOrFailExpr(
                            fieldName,
                            objTerm,
                            '{ new Property[inner](decoded) }.asTerm
                          )
                        }
                      } else {
                        current.set(decoded)
                      }
                    }.asExprOf[Unit]

              else if selectedType <:< TypeRepr.of[ListProperty[?]] then
                selectedType.asType match
                  case '[ListProperty[inner]] =>
                    '{
                      val current = ${ Select.unique(objTerm, fieldName).asExprOf[ListProperty[inner] | Null] }

                      if (current == null) {
                        val fresh = new ListProperty[inner]()
                        PropertyMacroRuntime.setListPropertyValue(fresh, $valueExpr)
                        ${
                          assignOrFailExpr(
                            fieldName,
                            objTerm,
                            '{ fresh }.asTerm
                          )
                        }
                      } else {
                        PropertyMacroRuntime.setListPropertyValue(current, $valueExpr)
                      }
                    }.asExprOf[Unit]

              else
                directAssignExpr(fieldName, objTerm, '{ value }.asTerm)
            }
        }
      }

    case _ =>
      report.error("Selector must be a simple field access like _.fieldName")
      '{???}
  }
}

private object PropertyMacroRuntime {
  def setListPropertyValue[V](list: ListProperty[V], value: Any): Unit =
    value match {
      case same: ListProperty[?] if same.eq(list) =>
        ()
      case same: ListProperty[?] =>
        list.setAll(same.get.iterator.asInstanceOf[Iterator[V]])
      case array: js.Array[?] =>
        list.setAll(array.iterator.asInstanceOf[Iterator[V]])
      case iterable: IterableOnce[?] =>
        list.setAll(iterable.iterator.asInstanceOf[Iterator[V]])
      case null =>
        list.clear()
      case other =>
        list.setAll(Iterator.single(other.asInstanceOf[V]))
    }
}
