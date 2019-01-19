package com.serebit.autotitan

import com.serebit.autotitan.cli.Cli

const val NAME = "AutoTitan"
const val VERSION = "1.0.0"
val config by lazy { BotConfig.generate() }

fun main(args: Array<String>) = Cli().main(args)
