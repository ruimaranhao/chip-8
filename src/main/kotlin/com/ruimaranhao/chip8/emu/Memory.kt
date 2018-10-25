package com.ruimaranhao.chip8.emu

import org.apache.commons.io.IOUtils
import java.io.*
import java.util.logging.Logger


class Memory {

    companion object {
        private val LOGGER = Logger.getLogger(Memory::class.java.name)

        private const val MEMORY_4K = 0x1000

        private const val OFFSET = 0x200

        private val fonts = intArrayOf(
                0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
                0x20, 0x60, 0x20, 0x20, 0x70, // 1
                0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
                0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
                0x90, 0x90, 0xF0, 0x10, 0x10, // 4
                0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
                0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
                0xF0, 0x10, 0x20, 0x40, 0x40, // 7
                0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
                0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
                0xF0, 0x90, 0xF0, 0x90, 0x90, // A
                0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
                0xF0, 0x80, 0x80, 0x80, 0xF0, // C
                0xE0, 0x90, 0x90, 0x90, 0xE0, // D
                0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
                0xF0, 0x80, 0xF0, 0x80, 0x80  // F
        )

        private var memory = ByteArray(MEMORY_4K) { _ -> 0}
    }

    init {

        for (i in 0 until fonts.size) {
            memory[i] = fonts[i].toByte()
        }
    }

    fun read(location: Int): Short {
        if (location > MEMORY_4K || location < 0) {
            throw IllegalArgumentException("Memory access out of bounds.")
        }

        return (memory[location].toInt() and 0xFF).toShort()
    }

    fun write(value: Int, location: Int) {
        if (location > MEMORY_4K || location < 0) {
            throw IllegalArgumentException("Memory access out of bounds.")
        }

        memory[location] = (value and 0xFF).toByte()
    }

    fun loadRom(file : String): Boolean {
        FileInputStream(File(file)).use {
            try {
                val data = IOUtils.toByteArray(it)
                var currentOffset = OFFSET
                for (theByte in data) {
                    write(theByte.toInt(), currentOffset)
                    currentOffset++
                }
            } catch (e: Exception) {
                LOGGER.severe("Error reading from stream")
                LOGGER.severe(e.message)
                return false
            }
        }

        return true
    }

}