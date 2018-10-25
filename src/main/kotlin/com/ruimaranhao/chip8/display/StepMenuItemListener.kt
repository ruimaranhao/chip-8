package com.ruimaranhao.chip8.display

import com.ruimaranhao.chip8.emu.EMU
import javax.swing.*
import java.awt.event.ItemEvent
import java.awt.event.ItemListener


class StepMenuItemListener(
        private val emulator: EMU) : ItemListener {

    override fun itemStateChanged(e: ItemEvent) {
        val button = e.source as AbstractButton
        if (!button.model.isSelected) {
            emulator.step = false
        } else {
            emulator.step = true
            emulator.trace = true
        }
    }
}