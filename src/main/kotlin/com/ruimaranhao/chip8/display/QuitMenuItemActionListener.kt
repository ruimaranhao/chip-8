package com.ruimaranhao.chip8.display

import javax.swing.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

class QuitMenuItemActionListener : ActionListener {
    override fun actionPerformed(e: ActionEvent) {
        val result = JOptionPane.showConfirmDialog(
                null,
                "Are you sure you want to quit?",
                "Bye, Bye... No!",
                JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.OK_OPTION) {
            System.exit(0)
        }
    }
}