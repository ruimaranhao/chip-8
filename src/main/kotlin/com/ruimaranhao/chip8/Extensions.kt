package com.ruimaranhao.chip8

import java.util.*

fun Byte.toPositiveInt() = toInt() and 0xFF
fun IntRange.random() = Random().nextInt((endInclusive + 1) - start) +  start

val Int.hex: String get() = Integer.toHexString(this)
val Byte.hex: String get() = Integer.toHexString(this.toInt())
