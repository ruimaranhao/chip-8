package com.ruimaranhao.chip8.emu

import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

class Keyboard : KeyAdapter() {

    // The current key being pressed, 0 if no key
    var currentKey = 0
        private set

    private var debugKeyPressed: Int = 0

    val debugKey: Int
        get() {
            val debugKey = debugKeyPressed
            debugKeyPressed = 0
            return debugKey
        }

    override fun keyPressed(e: KeyEvent?) {
        debugKeyPressed = e!!.keyCode

        when (debugKeyPressed) {
            CHIP8_QUIT -> System.exit(0)

            else -> currentKey = mapKeycodeToChip8Key(e.keyCode)
        }

        println(currentKey)
    }

    override fun keyReleased(e: KeyEvent?) {
        currentKey = 0
    }

    fun mapKeycodeToChip8Key(keycode: Int): Int {
        println("KC: " + keycode)
        for (i in sKeycodeMap.indices) {
            if (sKeycodeMap[i] == keycode) {
                return i + 1
            }
        }
        return 0
    }

    companion object {
         val sKeycodeMap = intArrayOf(
                 KeyEvent.VK_4, // Key 1
                 KeyEvent.VK_5, // Key 2
                 KeyEvent.VK_6, // Key 3
                 KeyEvent.VK_7, // Key 4
                 KeyEvent.VK_R, // Key 5
                 KeyEvent.VK_Y, // Key 6
                 KeyEvent.VK_U, // Key 7
                 KeyEvent.VK_F, // Key 8
                 KeyEvent.VK_G, // Key 9
                 KeyEvent.VK_H, // Key A
                 KeyEvent.VK_J, // Key B
                 KeyEvent.VK_V, // Key C
                 KeyEvent.VK_B, // Key D
                 KeyEvent.VK_N, // Key E
                 KeyEvent.VK_M) // Key F

        // The key to quit the emulator
        private const val CHIP8_QUIT = KeyEvent.VK_ESCAPE

        // The key to enter debug mode
        const val CHIP8_STEP = KeyEvent.VK_Z

        // The key to enter trace mode
        const val CHIP8_TRACE = KeyEvent.VK_X

        // The key to stop trace or debug
        const val CHIP8_NORMAL = KeyEvent.VK_C

        // The key to advance to the next instruction
        const val CHIP8_NEXT = KeyEvent.VK_N
    }
}