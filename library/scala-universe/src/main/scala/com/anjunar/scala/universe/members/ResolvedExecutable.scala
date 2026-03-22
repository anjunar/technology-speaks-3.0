package com.anjunar.scala.universe.members

import com.anjunar.scala.universe.ResolvedClass

import java.lang.reflect.Executable

abstract class ResolvedExecutable(override val underlying : Executable, owner : ResolvedClass) extends ResolvedMember(underlying, owner) {
  
  lazy val parameters : Array[ResolvedParameter] = underlying.getParameters.map(parameter => new ResolvedParameter(parameter, this))

}
