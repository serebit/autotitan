package com.serebit.autotitan.data

import com.serebit.autotitan.annotations.CommandFunction
import java.lang.reflect.Method

class Command(val instance: Any, val method: Method, info: CommandFunction) {
  val parameterTypes: MutableList<Class<*>>
  val name = when (info.name) {
    "" -> method.name.toLowerCase()
    else -> info.name
  }
  val description = info.description
  val delimitFinalParameter = info.delimitFinalParameter
  val serverOnly = info.serverOnly

  init {
    val parameterList = method.parameterTypes.toMutableList()
    parameterList.removeAt(0)
    parameterTypes = parameterList
  }
}