package com.ruimaranhao.chip8

import com.ruimaranhao.chip8.emu.EMU

fun main(args: Array<String>) {
    val emu = EMU(rom = "roms/INVADER")
    emu.start()
}