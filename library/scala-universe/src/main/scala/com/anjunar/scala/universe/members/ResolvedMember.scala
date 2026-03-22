package com.anjunar.scala.universe.members

import com.anjunar.scala.universe.ResolvedClass
import com.anjunar.scala.universe.annotations.Annotated

import java.lang.reflect.{Member, Type}

abstract class ResolvedMember(val underlying : Member, val owner : ResolvedClass) extends Annotated
