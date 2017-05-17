package com.serebit.autotitan.data

abstract class Extension() {
  fun writeData(filename: String, object: Any) {
    TODO("Add data write logic.")
  }
  
  fun readData(filename: String, type: Class<*>) {
    TODO("Add data read logic.")
  }
}
