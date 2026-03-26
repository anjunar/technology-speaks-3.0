package com.anjunar.technologyspeaks.security

import com.anjunar.json.mapper.schema.Link
import com.anjunar.json.mapper.provider.EntityProvider
import com.anjunar.technologyspeaks.{SpringContext, toKebabCase}
import jakarta.annotation.security.RolesAllowed
import org.springframework.web.bind.annotation.*
import org.springframework.web.util.UriComponentsBuilder
import java.lang.reflect.Method
import scala.quoted.*

class LinkBuilder(
                   val href: String,
                   var rel: String,
                   val method: String,
                   val function: Method
                 ) {

  var withId: Boolean = false
  val variables: java.util.Map[String, Any] = new java.util.HashMap[String, Any]()

  def withVariable(name: String, value: Any): LinkBuilder = {
    if (value != null) variables.put(name, value)
    this
  }

  def withRel(rel: String): LinkBuilder = {
    this.rel = rel
    this
  }

  def withId(value: Boolean): LinkBuilder = {
    this.withId = value
    this
  }

  def build(): Link = {
    // Falls Rollen-Prüfung fehlschlug oder Mapping fehlt
    if (href == null) return null

    val uriString = UriComponentsBuilder
      .fromPath("/")
      .path(href)
      .buildAndExpand(variables)
      .toUriString

    val actualRel = if (rel == null) {
      if (function != null) function.getName else "self"
    } else rel

    val linkId =
      if (!withId || function == null) null
      else function.getDeclaringClass.getSimpleName.toKebabCase().replace("-controller", "") + "-" + function.getName

    new Link(actualRel, uriString, method, linkId)
  }
}

object LinkBuilder {

  inline def create[C](inline call: C => Any): LinkBuilder =
    ${ createMacroImpl[C]('call) }

  private def createMacroImpl[C: Type](callExpr: Expr[C => Any])(using q: Quotes): Expr[LinkBuilder] = {
    import q.reflect.*

    // 1. Robuste Suche nach dem Methoden-Symbol und den Argumenten
    val (methodSym, argExprs) = extractMethodCall(callExpr)

    // 2. Mapping-Infos zur Kompilierzeit extrahieren
    val (httpMethod, hrefTemplate) = extractMappingAnnotation(methodSym)

    // 3. Parameter-Namen aus @PathVariable oder @RequestParam ziehen
    val paramBindings = extractParameters(methodSym, argExprs)

    // 4. Pfad-Variablen aus hrefTemplate extrahieren
    val pathVariables = extractPathVariables(hrefTemplate)

    val methodName = methodSym.name
    // fullName ist wichtig für Class.forName zur Laufzeit
    val controllerClassName = TypeRepr.of[C].typeSymbol.fullName

    val boundNames = paramBindings.map(_._1).toSet
    val unboundPathVariables = pathVariables.filterNot(boundNames.contains)

    val extraBindings = unboundPathVariables.flatMap { varName =>
        paramBindings.collectFirst {
            case (_, expr) if hasField(expr.asTerm.tpe, varName) =>
                val fieldTerm = accessField(expr.asTerm, varName)
                (varName, fieldTerm)
        }
    }

    '{
      val cls = Class.forName(${Expr(controllerClassName)})
      val javaMethod: Method = cls.getMethods
        .find(_.getName == ${Expr(methodName)})
        .orNull

      val builder = checkRolesAndGenerate(javaMethod, ${Expr(hrefTemplate)}, ${Expr(httpMethod)})

      // Generiert builder.withVariable(name, value) Aufrufe für jeden Parameter
      ${
        val paramVariableCalls = paramBindings.map { case (name, expr) =>
          val term = expr.asTerm
          val finalValueExpr = if (term.tpe <:< TypeRepr.of[Null]) {
            expr
          } else if (term.tpe <:< TypeRepr.of[EntityProvider]) {
            '{ ${expr.asExprOf[EntityProvider]}.id.toString }
          } else {
            expr
          }
          '{ builder.withVariable(${Expr(name)}, $finalValueExpr) }
        }

        val extraVariableCalls = extraBindings.map { case (name, fieldTerm) =>
            val finalFieldExpr = if (fieldTerm.tpe <:< TypeRepr.of[Null]) {
                fieldTerm.asExpr
            } else if (fieldTerm.tpe <:< TypeRepr.of[EntityProvider]) {
                '{ ${fieldTerm.asExprOf[EntityProvider]}.id.toString }
            } else {
                fieldTerm.asExpr
            }
            '{ builder.withVariable(${Expr(name)}, $finalFieldExpr) }
        }

        Expr.block(paramVariableCalls ++ extraVariableCalls, '{ builder })
      }
    }
  }

  private def extractPathVariables(template: String): List[String] = {
    "\\{([^}]+)}".r.findAllMatchIn(template).map(_.group(1)).toList
  }

  private def hasField(using q: Quotes)(tpe: q.reflect.TypeRepr, name: String): Boolean = {
    import q.reflect.*
    val sym = tpe.typeSymbol.fieldMember(name)
    if (sym.isNoSymbol) tpe.typeSymbol.methodMember(name).nonEmpty
    else true
  }

  private def accessField(using q: Quotes)(term: q.reflect.Term, name: String): q.reflect.Term = {
    import q.reflect.*
    val sym = term.tpe.typeSymbol.fieldMember(name)
    val finalSym = if (sym.isNoSymbol) term.tpe.typeSymbol.methodMember(name).head else sym
    Select(term, finalSym)
  }

  private def extractMethodCall(using q: Quotes)(expr: Expr[Any]): (q.reflect.Symbol, List[Expr[Any]]) = {
    import q.reflect.*

    // Rekursive Suche, um durch Inlined/Lambda/Block Schichten zu dringen
    def search(term: Term): (Symbol, List[Term]) = term match {
      case Inlined(_, _, inner) => search(inner)
      case Lambda(_, body) => search(body)
      case Block(Nil, last) => search(last)
      case Typed(inner, _) => search(inner)
      case Apply(fun, args) => (fun.symbol, args)
      case Select(_, _) => (term.symbol, Nil)
      case _ =>
        report.errorAndAbort(s"Struktur nicht unterstützt: ${term.show}. Bitte nutzen Sie _.methode(...)")
    }

    val (sym, args) = search(expr.asTerm)
    (sym, args.map(_.asExpr))
  }

  private def extractMappingAnnotation(using q: Quotes)(methodSym: q.reflect.Symbol): (String, String) = {
    import q.reflect.*

    val annotations = methodSym.annotations
    val mapping = annotations.collectFirst {
      case ann if ann.tpe <:< TypeRepr.of[GetMapping]    => ("GET", extractPath(ann))
      case ann if ann.tpe <:< TypeRepr.of[PostMapping]   => ("POST", extractPath(ann))
      case ann if ann.tpe <:< TypeRepr.of[PutMapping]    => ("PUT", extractPath(ann))
      case ann if ann.tpe <:< TypeRepr.of[DeleteMapping] => ("DELETE", extractPath(ann))
    }

    mapping.getOrElse(report.errorAndAbort(s"Methode '${methodSym.name}' hat keine Spring @*Mapping Annotation."))
  }

  private def extractPath(using q: Quotes)(ann: q.reflect.Term): String = {
    import q.reflect.*

    val strings = new scala.collection.mutable.ListBuffer[String]
    val accumulator = new TreeAccumulator[scala.collection.mutable.ListBuffer[String]] {
      override def foldTree(x: scala.collection.mutable.ListBuffer[String], tree: Tree)(owner: Symbol): scala.collection.mutable.ListBuffer[String] = {
        tree match {
          case NamedArg(name, inner) =>
            if (name == "value" || name == "path") foldOverTree(x, inner)(owner)
            else x
          case Literal(StringConstant(s)) =>
            if (s.nonEmpty) x += s
            x
          case _ => foldOverTree(x, tree)(owner)
        }
      }
    }
    accumulator.foldTree(strings, ann)(Symbol.spliceOwner)
    if (strings.isEmpty) report.errorAndAbort(s"Mapping-Annotation ohne 'value' oder 'path' Parameter: ${ann.show}")
    strings.find(_.startsWith("/")).getOrElse(strings.head)
  }

  private def extractParameters(using q: Quotes)(methodSym: q.reflect.Symbol, argExprs: List[Expr[Any]]): List[(String, Expr[Any])] = {
    import q.reflect.*
    val params = methodSym.paramSymss.flatten
    params.zip(argExprs).map { (param, expr) =>
      val name = param.annotations.collectFirst {
        case ann if ann.tpe <:< TypeRepr.of[PathVariable] => extractAnnotationValue(ann)
        case ann if ann.tpe <:< TypeRepr.of[RequestParam] => extractAnnotationValue(ann)
      }.flatten.getOrElse(param.name)
      (name, expr)
    }
  }

  private def extractAnnotationValue(using q: Quotes)(ann: q.reflect.Term): Option[String] = {
    import q.reflect.*
    ann match {
      case Apply(_, args) => args.collectFirst {
        case NamedArg("value" | "name", Literal(StringConstant(v))) => v
        case Literal(StringConstant(v)) => v
      }
      case _ => None
    }
  }

  // Runtime-Logik (Muss public sein, damit das Makro sie aufrufen kann)
  def checkRolesAndGenerate(javaMethod: Method, href: String, httpMethod: String): LinkBuilder = {
    if (javaMethod == null) return new LinkBuilder(null, null, null, null)

    val rolesAllowed = javaMethod.getAnnotation(classOf[RolesAllowed])
    val hasAccess = if (rolesAllowed == null) true else {
      val identityHolder = SpringContext.getBean(classOf[IdentityHolder])
      rolesAllowed.value().exists(role => identityHolder.hasRole(role))
    }

    if (hasAccess) {
      new LinkBuilder(href, null, httpMethod, javaMethod)
    } else {
      new LinkBuilder(null, null, null, null)
    }
  }
}
