package com.ruimaranhao.chip8.display


import com.ruimaranhao.chip8.emu.EMU
import javax.swing.*
import java.awt.event.ItemEvent
import java.awt.event.ItemListener

class TraceMenuItemListener(private val emulator: EMU) : ItemListener {

    override fun itemStateChanged(e: ItemEvent) {
        val button = e.source as AbstractButton
        emulator.trace = button.model.isSelected
    }
}