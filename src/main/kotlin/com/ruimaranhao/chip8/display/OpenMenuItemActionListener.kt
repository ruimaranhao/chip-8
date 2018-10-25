package com.ruimaranhao.chip8.display

import com.ruimaranhao.chip8.emu.EMU
import java.awt.event.ActionEvent
import java.awt.event.ActionListener


class OpenMenuItemActionListener(private val emulator: EMU) : ActionListener {

    override fun actionPerformed(e: ActionEvent) {
        emulator.loadFile()
    }
}