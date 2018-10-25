package com.ruimaranhao.chip8.display

class ScreenMode(
        val width: Int,
        val height: Int,
        val scale: Int) {

    init {
        if (scale < 1) {
            throw IllegalArgumentException("scale must be >= 1")
        }
    }

}