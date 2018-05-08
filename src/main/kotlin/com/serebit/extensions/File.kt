@file:JvmName("FileExtensions")

package com.serebit.extensions

import java.io.File
import java.nio.file.Files

val File.contentType: String get() = Files.probeContentType(toPath())
