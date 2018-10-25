package com.ruimaranhao.chip8.display

import com.ruimaranhao.chip8.emu.CPU
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

class ResetMenuItemActionListener(
        private val cpu: CPU) : ActionListener {

    override fun actionPerformed(e: ActionEvent) {
       cpu.reset()
    }
}