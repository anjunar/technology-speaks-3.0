package com.anjunar.json.mapper.macros

import com.anjunar.json.mapper.macros.{PropertyAccess, RuntimeAnnotationSupport, RuntimeTypeResolver}

import java.lang.annotation.Annotation
import java.lang.reflect.Type as JType
import scala.annotation.tailrec
import scala.quoted.*

object PropertyMacros {

  inline def makePropertyAccess[E, V](inline selector: E => V): PropertyAccess[E, V] =
    ${ makePropertyAccessImpl[E, V]('selector) }

  private def makePropertyAccessImpl[E: Type, V: Type](
                                                        selectorExpr: Expr[E => V]
                                                      )(using Quotes): Expr[PropertyAccess[E, V]] = {
    import quotes.reflect.*

    final case class SelectedProperty(
                                       name: String,
                                       symbol: Symbol,
                                       qualifierType: TypeRepr
                                     )

    def normalizeType(tpe: TypeRepr): TypeRepr = {
      @tailrec
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

    @tailrec
    def unwrapTerm(term: Term): Term =
      term match {
        case Inlined(_, _, inner) => unwrapTerm(inner)
        case Typed(inner, _)      => unwrapTerm(inner)
        case Block(Nil, expr)     => unwrapTerm(expr)
        case other                => other
      }

    @tailrec
    def selectedPropertyFromBody(body: Term, paramSym: Symbol): SelectedProperty =
      body match {
        case Inlined(_, _, inner) =>
          selectedPropertyFromBody(inner, paramSym)

        case Typed(inner, _) =>
          selectedPropertyFromBody(inner, paramSym)

        case Select(qualifier, fieldName) =>
          val unwrappedQualifier = unwrapTerm(qualifier)
          if unwrappedQualifier.symbol == paramSym then
            SelectedProperty(
              fieldName,
              body.symbol,
              normalizeType(unwrappedQualifier.tpe)
            )
          else
            report.errorAndAbort(
              s"Expected a simple field selection such as _.nickName, but found:\n${body.show(using Printer.TreeStructure)}"
            )

        case _ =>
          report.errorAndAbort(
            s"Expected a simple field selection such as _.nickName, but found:\n${body.show(using Printer.TreeStructure)}"
          )
      }

    def propertyFromSelector(expr: Expr[E => V]): SelectedProperty = {
      @tailrec
      def loop(term: Term, localDefs: Map[String, DefDef]): SelectedProperty =
        term match {
          case Inlined(_, _, inner) =>
            loop(inner, localDefs)

          case Block(stats, expr) =>
            val defs = stats.collect { case dd: DefDef => dd.name -> dd }.toMap
            loop(expr, localDefs ++ defs)

          case Lambda(List(param), body) =>
            selectedPropertyFromBody(body, param.symbol)

          case Closure(Ident(name), _) =>
            localDefs.get(name) match {
              case Some(dd) =>
                dd.rhs match {
                  case Some(rhs) =>
                    val params = dd.termParamss.flatMap(_.params)
                    params match {
                      case param :: Nil =>
                        selectedPropertyFromBody(rhs, param.symbol)

                      case _ =>
                        report.errorAndAbort(
                          s"Closure $name does not have exactly one parameter:\n${dd.show(using Printer.TreeStructure)}"
                        )
                    }

                  case None =>
                    report.errorAndAbort(
                      s"Closure $name has no body:\n${dd.show(using Printer.TreeStructure)}"
                    )
                }

              case None =>
                report.errorAndAbort(
                  s"Could not resolve closure $name in the surrounding block.\n${term.show(using Printer.TreeStructure)}"
                )
            }

          case _ =>
            report.errorAndAbort(
              s"Expected a simple selector such as _.nickName, but found:\n${term.show(using Printer.TreeStructure)}"
            )
        }

      loop(expr.asTerm, Map.empty)
    }

    def resolvedSelectedType(selected: SelectedProperty): TypeRepr = {
      val qualifierType = normalizeType(selected.qualifierType)
      val declaringOwner = selected.symbol.owner

      val contextualOwner =
        qualifierType.baseType(declaringOwner) match {
          case bt if bt != NoPrefix => normalizeType(bt)
          case _                    => qualifierType
        }

      normalizeType(contextualOwner.memberType(selected.symbol))
    }

    def runtimeClassExpr(tpe: TypeRepr): Expr[Class[?]] = {
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

          '{ RuntimeTypeResolver.resolveClass(${ Expr(sym.fullName) }) }
      }
    }

    def runtimeTypeExpr(tpe: TypeRepr): Expr[JType] = {
      normalizeType(tpe) match {
        case tr: TypeRef if tr.typeSymbol.flags.is(Flags.Param) =>
          val name = tr.typeSymbol.name
          '{ RuntimeTypeResolver.typeVariable(${ Expr(name) }) }

        case AppliedType(rawType, args) =>
          val rawClassExpr = runtimeClassExpr(rawType)
          val argExprs: Seq[Expr[JType]] = args.map(runtimeTypeExpr)
          val argArrayExpr: Expr[Array[JType]] = '{ ${ Expr.ofSeq(argExprs) }.toArray }

          '{
            RuntimeTypeResolver.parameterized(
              $rawClassExpr,
              $argArrayExpr
            )
          }

        case t if t <:< TypeRepr.of[Array[?]] =>
          t.asType match {
            case '[Array[a]] =>
              val componentExpr = runtimeTypeExpr(TypeRepr.of[a])
              '{
                val component = $componentExpr
                component match
                  case cls: Class[?] =>
                    java.lang.reflect.Array.newInstance(cls, 0).getClass.asInstanceOf[JType]
                  case other =>
                    RuntimeTypeResolver.genericArray(other)
              }
          }

        case other =>
          val clsExpr = runtimeClassExpr(other)
          '{ $clsExpr: JType }
      }
    }

    def setterExpr(propertyName: String): Expr[(E, V) => Unit] = {
      val setterName = s"${propertyName}_="

      Lambda(
        Symbol.spliceOwner,
        MethodType(List("instance", "value"))(
          _ => List(TypeRepr.of[E], TypeRepr.of[V]),
          _ => TypeRepr.of[Unit]
        ),
        (owner, params) => {
          val instanceTerm = params.head.asInstanceOf[Term]
          val valueTerm = params(1).asInstanceOf[Term]

          val setterSym =
            instanceTerm.tpe.widen.typeSymbol.methodMember(setterName).headOption.getOrElse(Symbol.noSymbol)

          if setterSym != Symbol.noSymbol then
            Apply(Select(instanceTerm, setterSym), List(valueTerm.changeOwner(owner)))
          else
            '{
              throw new UnsupportedOperationException(
                ${ Expr(s"Property '$propertyName' is read-only. No Scala setter '$setterName' was found.") }
              )
            }.asTerm.changeOwner(owner)
        }
      ).asExprOf[(E, V) => Unit]
    }

    def constantValueExpr(term: Term): Expr[Any] =
      term match {
        case Literal(IntConstant(v))       => Expr(v)
        case Literal(LongConstant(v))      => Expr(v)
        case Literal(FloatConstant(v))     => Expr(v)
        case Literal(DoubleConstant(v))    => Expr(v)
        case Literal(BooleanConstant(v))   => Expr(v)
        case Literal(StringConstant(v))    => Expr(v)
        case Literal(ByteConstant(v))      => Expr(v)
        case Literal(ShortConstant(v))     => Expr(v)
        case Literal(CharConstant(v))      => Expr(v)
        case Literal(NullConstant())       => '{ null }
        case Literal(ClassOfConstant(tpe)) => runtimeClassExpr(tpe)
        case _ =>
          report.errorAndAbort(
            s"Only literal annotation arguments are currently supported, but found:\n${term.show(using Printer.TreeStructure)}"
          )
      }

    def annotationValueExpr(term: Term): Option[Expr[Any]] = {
      def loop(t: Term): Option[Expr[Any]] = {
        val unwrapped = unwrapTerm(t)

        unwrapped match {
          case NamedArg(_, value) =>
            loop(value)

          case Inlined(_, _, inner) =>
            loop(inner)

          case Block(Nil, expr) =>
            loop(expr)

          case Typed(inner, _) =>
            loop(inner)

          case Literal(_) =>
            Some(constantValueExpr(unwrapped))

          case Repeated(elems, _) =>
            val elemExprs = elems.flatMap(loop)
            if (elemExprs.size == elems.size) then
              Some('{ ${ Expr.ofSeq(elemExprs) }.toArray })
            else
              None

          case Select(_, member) if unwrapped.tpe <:< TypeRepr.of[java.lang.Enum[?]] =>
            val enumClassExpr = runtimeClassExpr(unwrapped.tpe)
            Some('{
              $enumClassExpr.getField(${ Expr(member) }).get(null).asInstanceOf[Any]
            })

          case Wildcard() =>
            Some('{ null })

          case TypeApply(inner, _) =>
            loop(inner)

          case Apply(Apply(TypeApply(Select(Ident("Array"), "apply"), _), List(Typed(Repeated(elems, _), _))), _) =>
            val elemExprs = elems.flatMap(loop)
            if (elemExprs.size == elems.size) then
              Some('{ ${ Expr.ofSeq(elemExprs) }.toArray })
            else
              None

          case _ =>
            report.warning(s"Unsupported annotation argument:\n${unwrapped.show(using Printer.TreeStructure)}")
            None
        }
      }

      loop(term)
    }
    
    def annotationDescriptorExpr(annotation: Term): Option[Expr[RuntimeAnnotationSupport.AnnotationDescriptor]] = {
      val ann = unwrapTerm(annotation)

      ann match {
        case Apply(Select(New(tpt), _), args) =>
          val annotationType = tpt.tpe.typeSymbol
          val annotationClassName = annotationType.fullName

          val values: Seq[Option[Expr[RuntimeAnnotationSupport.AnnotationValue]]] =
            args.zipWithIndex.map { case (arg, idx) =>
              val (name, value) = arg match {
                case NamedArg(n, v) => (Some(n), v)
                case other =>
                  val methods = annotationType.methodMembers.filter(m => m.paramSymss.flatten.isEmpty)
                  (methods.lift(idx).map(_.name), other)
              }

              for {
                n <- name
                v <- annotationValueExpr(value)
              } yield '{
                RuntimeAnnotationSupport.AnnotationValue(
                  ${ Expr(n) },
                  $v
                )
              }
            }

          if (values.forall(_.isDefined)) then
            val definedValues = values.flatten
            Some('{
              RuntimeAnnotationSupport.AnnotationDescriptor(
                ${ Expr(annotationClassName) },
                ${ Expr.ofSeq(definedValues) }.toArray
              )
            })
          else
            None

        case _ =>
          None
      }
    }

    def annotationsExpr(selected: SelectedProperty): Expr[Array[? <: Annotation]] = {
      val annTerms = selected.symbol.annotations

      val descriptorExprs =
        annTerms.flatMap(annotationDescriptorExpr)

      '{
        RuntimeAnnotationSupport.instantiateAll(
          ${ Expr.ofSeq(descriptorExprs) }.toArray
        )
      }
    }

    val selected = propertyFromSelector(selectorExpr)
    val propertyName = selected.name
    val selectedType = resolvedSelectedType(selected)
    val genericTypeExpr = runtimeTypeExpr(selectedType)
    val setterLambdaExpr = setterExpr(propertyName)
    val annotationsArrayExpr = annotationsExpr(selected)

    '{
      new PropertyAccess[E, V] {

        override val name: String =
          ${ Expr(propertyName) }

        override val annotations: Array[? <: Annotation] =
          $annotationsArrayExpr

        override val genericType: JType =
          $genericTypeExpr

        override def get(instance: E): V =
          $selectorExpr(instance)

        override def set(instance: E, value: V): Unit =
          $setterLambdaExpr(instance, value)
      }
    }
  }
}