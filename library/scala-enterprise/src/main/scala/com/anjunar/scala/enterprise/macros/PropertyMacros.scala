package com.anjunar.scala.enterprise.macros

import java.lang.annotation.Annotation
import scala.annotation.tailrec
import scala.quoted.*
import com.anjunar.scala.enterprise.macros.reflection.{SimpleClass, Type => JType, ParameterizedType, GenericArrayType}

object PropertyMacros {

  inline def makePropertyAccess[E, V](inline selector: E => V): PropertyAccess[E, V] =
    ${ makePropertyAccessImpl[E, V]('selector) }

  inline def describeProperties[E]: List[PropertyAccess[E, ?]] =
    ${ describePropertiesImpl[E] }

  private def describePropertiesImpl[E: Type](using Quotes): Expr[List[PropertyAccess[E, ?]]] = {
    import quotes.reflect.*

    val typeRepr = TypeRepr.of[E]
    val symbol = typeRepr.typeSymbol

    val members = symbol.fieldMembers ++ symbol.methodMembers
    
    val propertySymbols = members.filter { s =>
      val flags = s.flags
      !flags.is(Flags.Private) &&
        !flags.is(Flags.Protected) &&
        !flags.is(Flags.Artifact) &&
        !flags.is(Flags.Macro) &&
        !s.name.contains("$") &&
        !s.name.endsWith("_=") &&
        (s.isTerm && (!flags.is(Flags.Method) || (flags.is(Flags.ParamAccessor) || (flags.is(Flags.Method) && s.paramSymss.isEmpty))))
    }.distinctBy(_.name)

    val filteredSymbols = propertySymbols.filter { s =>
      val tpe = normalizeType(typeRepr.memberType(s))
      val isMethod = tpe match {
        case _: MethodType => true
        case _: PolyType => true
        case _ => false
      }
      !isMethod && !s.isClassConstructor && s.name != "hashCode" && s.name != "toString" && s.name != "getClass" && s.name != "clone" && s.name != "notify" && s.name != "notifyAll" && s.name != "wait" && s.name != "finalize"
    }

    val propertyExprs = filteredSymbols.map { s =>
      val name = s.name
      val tpe = normalizeType(typeRepr.memberType(s))
      val isWriteable = symbol.methodMember(s"${name}_=").nonEmpty
      
      tpe.asType match {
        case '[v] =>
          val nameExpr = Expr(name)
          val genericTypeExpr = runtimeTypeExpr(tpe)
          val isWriteableExpr = Expr(isWriteable)
          
          val annotationsExpr = {
            val annTerms = s.annotations
            val descriptorExprs = annTerms.flatMap(annotationDescriptorExpr)
            '{
              RuntimeAnnotationSupport.instantiateAll(
                Array(${Varargs(descriptorExprs)}*)
              )
            }
          }

          val getterExpr = Lambda(
            Symbol.spliceOwner,
            MethodType(List("instance"))(_ => List(typeRepr), _ => tpe),
            (owner, params) => {
              val instance = params.head.asInstanceOf[Term]
              Select(instance, s).changeOwner(owner)
            }
          ).asExprOf[E => v]

          val setterLambdaExpr = setterExpr[E, v](name)

          '{
            new PropertyAccess[E, v] {
              override val name: String = $nameExpr
              override val annotations: Array[? <: Annotation] = $annotationsExpr
              override val genericType: JType = $genericTypeExpr
              override val isWriteable: Boolean = $isWriteableExpr
              override def get(instance: E): v = $getterExpr(instance)
              override def set(instance: E, value: v): Unit = $setterLambdaExpr(instance, value)
            }
          }
      }
    }

    Expr.ofList(propertyExprs)
  }

  private def makePropertyAccessImpl[E: Type, V: Type](
                                                        selectorExpr: Expr[E => V]
                                                      )(using Quotes): Expr[PropertyAccess[E, V]] = {
    import quotes.reflect.*

    final case class SelectedProperty(
                                       name: String,
                                       symbol: Symbol,
                                       qualifierType: TypeRepr
                                     )

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

    val selected = propertyFromSelector(selectorExpr)
    val propertyName = selected.name
    val selectedType = resolvedSelectedType(selected)
    val genericTypeExpr = runtimeTypeExpr(selectedType)
    val setterLambdaExpr = setterExpr[E, V](propertyName)
    val isWriteable = selected.qualifierType.typeSymbol.methodMember(s"${propertyName}_=").nonEmpty
    val isWriteableExpr = Expr(isWriteable)
    
    val annotationsArrayExpr = {
        val annTerms = selected.symbol.annotations
        val descriptorExprs = annTerms.flatMap(annotationDescriptorExpr)
        '{
            RuntimeAnnotationSupport.instantiateAll(
                Array(${Varargs(descriptorExprs)}*)
            )
        }
    }

    '{
      new PropertyAccess[E, V] {

        override val name: String =
          ${ Expr(propertyName) }

        override val annotations: Array[? <: Annotation] =
          $annotationsArrayExpr

        override val genericType: JType =
          $genericTypeExpr

        override val isWriteable: Boolean =
          $isWriteableExpr

        override def get(instance: E): V =
          $selectorExpr(instance)

        override def set(instance: E, value: V): Unit =
          $setterLambdaExpr(instance, value)
      }
    }
  }

  private def normalizeType(using Quotes)(tpe: quotes.reflect.TypeRepr): quotes.reflect.TypeRepr = {
    import quotes.reflect.*
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
  private def unwrapTerm(using Quotes)(term: quotes.reflect.Term): quotes.reflect.Term = {
    import quotes.reflect.*
    term match {
      case Inlined(_, _, inner) => unwrapTerm(inner)
      case Typed(inner, _)      => unwrapTerm(inner)
      case Block(Nil, expr)     => unwrapTerm(expr)
      case other                => other
    }
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

        '{ RuntimeTypeResolver.resolveClass(${ Expr(sym.fullName) }) }
    }
  }

  private def runtimeTypeExpr(using Quotes)(tpe: quotes.reflect.TypeRepr): Expr[JType] = {
    import quotes.reflect.*
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
                  new SimpleClass(java.lang.reflect.Array.newInstance(cls, 0).getClass).asInstanceOf[JType]
                case other =>
                  RuntimeTypeResolver.genericArray(other)
            }
        }

      case other =>
        val clsExpr = runtimeClassExpr(other)
        '{ $clsExpr: JType }
    }
  }

  private def setterExpr[E: Type, V: Type](using Quotes)(propertyName: String): Expr[(E, V) => Unit] = {
    import quotes.reflect.*
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

  private def constantValueExpr(using Quotes)(term: quotes.reflect.Term): Expr[Any] = {
    import quotes.reflect.*
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
  }

  private def annotationValueExpr(using Quotes)(term: quotes.reflect.Term): Option[Expr[Any]] = {
    import quotes.reflect.*
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
          None
/*
          Some('{
            $enumClassExpr.getField(${ Expr(member) }).get(null).asInstanceOf[Any]
          })
*/

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
  
  private def annotationDescriptorExpr(using Quotes)(annotation: quotes.reflect.Term): Option[Expr[RuntimeAnnotationSupport.AnnotationDescriptor]] = {
    import quotes.reflect.*
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
              Array(${Varargs(definedValues)}*)
            )
          })
        else
          None

      case _ =>
        None
    }
  }

}
