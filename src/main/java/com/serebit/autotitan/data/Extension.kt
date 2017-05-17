package com.serebit.autotitan.data

abstract class Extension() {
  fun writeData(fileName: String, data: Any) {
    TODO("Add data write logic.")
  }
  
  fun readData(fileName: String, dataType: Class<*>) {
    TODO("Add data read logic.")
  }
}
