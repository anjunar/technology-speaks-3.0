package com.anjunar.scala.enterprise.macros

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

    // Collect property symbols from the class and all base classes
    def collectPropertySymbols(tpe: TypeRepr): List[Symbol] = {
      val allSymbols = tpe.baseClasses.flatMap { baseClass =>
        baseClass.declarations.collect {
          case s: Symbol if s.isTerm => s
        }.filter { s =>
          !s.flags.is(Flags.Private) && !s.flags.is(Flags.Protected) &&
          !s.name.contains("$") &&
          !s.name.endsWith("_=") &&
          s.name != "hashCode" && s.name != "toString" && s.name != "getClass" && s.name != "clone" && s.name != "notify" && s.name != "notifyAll" && s.name != "wait" && s.name != "finalize" &&
          !s.isClassConstructor &&
          !s.flags.is(Flags.Macro) &&
          !s.flags.is(Flags.Artifact)
        }.filter { s =>
          val isField = !s.isDefDef
          val isMethod = s.isDefDef

          if (isField) {
            true
          } else if (isMethod) {
            val methodType = normalizeType(tpe.memberType(s))
            val isParameterless = s.paramSymss.isEmpty
            val returnsUnit = methodType =:= TypeRepr.of[Unit]

            isParameterless && !returnsUnit
          } else {
            false
          }
        }
      }
      allSymbols.distinctBy(_.name)
    }

    val propertySymbols = collectPropertySymbols(typeRepr)

    val propertyExprs = propertySymbols.map { s =>
      val name = s.name
      val propertyType = normalizeType(typeRepr.memberType(s))
      val isWriteable = typeRepr.typeSymbol.methodMember(s"${name}_=").nonEmpty || 
        typeRepr.baseClasses.exists(_.methodMember(s"${name}_=").nonEmpty)

      propertyType.asType match {
        case '[v] =>
          val nameExpr = Expr(name)
          val genericTypeExpr = runtimeTypeExpr(propertyType)
          val isWriteableExpr = Expr(isWriteable)

          val annotationsExpr = extractAnnotations(s)

          val getterExpr = Lambda(
            Symbol.spliceOwner,
            MethodType(List("instance"))(_ => List(typeRepr), _ => propertyType),
            (owner, params) => {
              val instance = params.head.asInstanceOf[Term]
              Select(instance, s).changeOwner(owner)
            }
          ).asExprOf[E => v]

          val setterLambdaExpr = setterExpr[E, v](name)

          '{
            new PropertyAccess[E, v] {
              override val name: String = $nameExpr
              override val annotations: Array[Annotation] = $annotationsExpr
              override val genericType: JType = $genericTypeExpr
              override val isWriteable: Boolean = $isWriteableExpr
              def propertyType: Class[?] = null
              def valueType: Class[?] = null
              override def get(instance: E): v = $getterExpr(instance)
              override def set(instance: E, value: v): Unit = $setterLambdaExpr(instance, value)
            }
          }
      }
    }

    Expr.ofList(propertyExprs)
  }

  private def extractAnnotations(using Quotes)(symbol: quotes.reflect.Symbol): Expr[Array[Annotation]] = {
    import quotes.reflect.*
    
    val annotationExprs = symbol.annotations.flatMap { ann =>
      ann.tpe match {
        case AnnotatedType(_, _) | TypeRef(_, _) =>
          val className = ann.tpe.typeSymbol.fullName
          val params = extractAnnotationParams(ann)
          if params.isEmpty then
            Some('{ Annotation(${ Expr(className) }, Map.empty) })
          else
            val paramsExpr = '{ Map(${ Varargs(params.map { case (k, v) => '{ (${ Expr(k) }, $v) } }) }*) }
            Some('{ Annotation(${ Expr(className) }, $paramsExpr) })
        case _ => None
      }
    }
    
    '{ Array(${ Varargs(annotationExprs) }*) }
  }

  private def extractAnnotationParams(using Quotes)(ann: quotes.reflect.Term): List[(String, Expr[Any])] = {
    import quotes.reflect.*
    
    ann match {
      case Apply(_, args) =>
        val paramNames = ann.tpe.typeSymbol.primaryConstructor.paramSymss.flatten.map(_.name)
        args.zip(paramNames).collect {
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
      case _ => Nil
    }
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

    val annotationsArrayExpr = extractAnnotations(selected.symbol)

    '{
      new PropertyAccess[E, V] {

        override val name: String =
          ${ Expr(propertyName) }

        override val annotations: Array[Annotation] =
          $annotationsArrayExpr

        override val genericType: JType =
          $genericTypeExpr

        override val isWriteable: Boolean =
          $isWriteableExpr

        def propertyType: Class[?] = null
        def valueType: Class[?] = null

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
        val argArrayExpr: Expr[Array[JType]] = '{ Array[JType](${ Varargs(argExprs) }*) }

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
              RuntimeTypeResolver.genericArray($componentExpr)
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

}
