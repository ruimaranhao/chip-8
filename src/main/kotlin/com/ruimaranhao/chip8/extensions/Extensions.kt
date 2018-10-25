package com.ruimaranhao.chip8.extensions

import java.util.*

fun Byte.uInt() = toInt() and 0xFF

val Int.hex: String get() = Integer.toHexString(this)
val Short.hex: String get() = Integer.toHexString(this.toInt())
val Byte.hex: String get() = Integer.toHexString(this.toInt())

fun IntRange.random() = Random().nextInt((endInclusive + 1) - start) +  start
