package com.anjunar.json.mapper.macros

import scala.quoted.*

object SchemaProviderMacros {

  inline def instantiateSchema[E]: E =
    ${ SchemaProviderMacros.instantiateSchemaImpl[E] }

  def instantiateSchemaImpl[E: Type](using Quotes): Expr[E] = {
    import quotes.reflect.*

    val tpe = TypeRepr.of[E].dealias
    val sym = tpe.typeSymbol

    if !sym.isClassDef then
      report.errorAndAbort(s"${Type.show[E]} ist kein Klassen-Typ")

    if sym.flags.is(Flags.Abstract) then
      report.errorAndAbort(s"${Type.show[E]} ist abstrakt und kann nicht instanziiert werden")

    val ctor = sym.primaryConstructor
    val paramLists = ctor.paramSymss

    if paramLists.exists(_.nonEmpty) then
      report.errorAndAbort(
        s"${Type.show[E]} muss einen parameterlosen Primärkonstruktor haben. " +
          s"Gefunden wurden Parameterlisten: ${paramLists.map(_.map(_.name))}"
      )

    tpe.asType match
      case '[e] =>
        Apply(
          Select(
            New(TypeTree.of[e]),
            ctor
          ),
          Nil
        ).asExprOf[E]
  }
}