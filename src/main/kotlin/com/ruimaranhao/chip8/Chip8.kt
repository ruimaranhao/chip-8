package com.ruimaranhao.chip8

import java.io.File

const val MASK = 0xF000

class Chip8 {

    val fonts = intArrayOf(
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

    var pc: Int  = 0x200
    var opcode = 0
    var I = 0
    var sp = 0
    var dt = 0
    var st = 0

    var stack = Array(16) { _ -> 0}
    var keys = Array(16) { _ -> 0}
    var V = Array(16) { _ -> 0}
    var gfx = Array(64 * 32) { _ -> 0}

    lateinit var memory: ByteArray

    fun load_rom(file : String): Boolean {
        var rom = File(file).readBytes()

        memory = ByteArray(512 + rom.size)

        for (i in 0 until fonts.size) {
            memory[i] = fonts[i].toByte()
        }

        System.arraycopy(rom, 0, memory, 512, rom.size)

        return true
    }

    private fun addr(opcode: Int): Int {
        return opcode and 0x0FFF
    }

    private fun value(opcode: Int): Int {
        return opcode and 0x00FF
    }

    private fun getRegVx(opcode: Int): Int {
        return V[opcode and 0x0F00 shr 8]
    }

    private fun updateRegVx(opcode: Int, value: Int) {
        V[opcode and 0x0F00 shr 8] = value
    }

    private fun getRegVy(opcode: Int): Int {
        return V[opcode and 0x00F0 shr 4]
    }

    fun emulate() {
        opcode = (memory[pc].toPositiveInt() shl 8) or memory[pc + 1].toPositiveInt()
        pc += 2

        when(opcode and MASK) {
            0x0000 -> process_00E(opcode)

            0x1000 -> pc = addr(opcode)

            0x2000 -> {
                stack[sp++] = pc
                pc = addr(opcode)
            }

            0x3000 -> pc += if (getRegVx(opcode) == value(opcode)) 2 else 0

            0x4000 -> pc += if (getRegVx(opcode) != value(opcode)) 2 else 0

            0x5000 -> pc += if (getRegVx(opcode) == getRegVy(opcode)) 2 else 0

            0x6000 -> updateRegVx(opcode, value(opcode))

            0x7000 -> updateRegVx(opcode, getRegVx(opcode) + value(opcode))

            0x8000 -> process_8(opcode)

            0x9000 -> pc += if (getRegVx(opcode) != getRegVy(opcode)) 2 else 0

            0xA000 -> I = addr(opcode)

            0xB000 -> pc = addr(opcode) + V[0]

            0xC000 -> updateRegVx(opcode, (0..0xFF).random() % (0xFF + 1) and value(opcode))

            0xD000 -> drawSprite(opcode)

            0xE000 -> process_EX(opcode)

            0xF000 -> process_FX(opcode)

            else -> println("Unknown Main Opcode: " + Integer.toHexString(opcode and MASK))
        }

        if (dt > 0) {
            --dt
        }

        if (st > 0) {
            --st
        }

    }

    private fun drawSprite(opcode: Int) {
        val x = getRegVx(opcode)
        val y = getRegVy(opcode)
        var pixel : Byte

        V[0xF] = 0

        for(yline in 0 until (opcode and 0x000F)) {
            pixel = memory[I + yline]

            for(xline in 0..8) {
                if (pixel.toPositiveInt() and (0x80 shr xline) != 0) {
                    if (gfx[x + xline + (y + yline) * 64] == 1) {
                        V[0xF] = 1
                    }
                    gfx[x + xline + (y + yline) * 64] = gfx[x + xline + (y + yline) * 64] xor 1
                }
            }
        }
    }

    private fun process_00E(opcode: Int) {
        when (opcode) {
            0x00E0 -> {
                for (i in 0..64*32) {
                    gfx[i] = 0
                }
            }

            0x00EE -> pc = stack[--sp]

            else -> println("\nUnhandled opcode: $opcode\n")

        }
    }


    private fun process_FX(opcode: Int) {
        when(value(opcode)) {
            0x0007 -> updateRegVx(opcode, dt)

            0x000A -> {
                pc -= 2

                for (k in 0..15) {
                    if (keys[k] === 1) {
                        updateRegVx(opcode, k)
                        pc += 2
                    }
                }
            }
            0x0015 -> dt = getRegVx(opcode)

            0x0018 -> st = getRegVx(opcode)

            0x001E -> {
                V[0xF] = if (I + getRegVx(opcode) > 0xFFF) 1 else 0
                I += getRegVx(opcode)
            }

            0x0029 -> I = getRegVx(opcode) * 0x5

            0x0033 -> {
                memory[I] = (getRegVx(opcode) / 100).toByte()
                memory[I + 1] = (getRegVx(opcode) / 10 % 10).toByte()
                memory[I + 2] = (getRegVx(opcode) % 100 % 10).toByte()
            }

            0x0055 -> {
                for (i in 0..((opcode and 0x0F00) shr 8))
                    memory[I + i] = V[i].toByte()
            }

            0x0065 -> {
                for (i in 0..((opcode and 0x0F00) shr 8))
                    V[i] = memory[I + i].toPositiveInt()
            }

            else -> println("Unknown opcode [0xFXAB]: " + Integer.toHexString(opcode and 0x00FF))

        }
    }

    private fun process_EX(opcode: Int) {
        when(value(opcode)) {
            0x009E -> pc += if (keys[getRegVx(opcode)] != 0) 2 else 0

            0x00A1 -> pc += if (keys[getRegVx(opcode)] == 0) 2 else 0
        }
    }

    private fun process_8(opcode: Int) {
        when(opcode and 0x000F) {
            0x0000 -> updateRegVx(opcode, getRegVy(opcode))

            0x0001 -> updateRegVx(opcode, getRegVx(opcode) or getRegVy(opcode))

            0x0002 -> updateRegVx(opcode, getRegVx(opcode) and getRegVy(opcode))

            0x0003 -> updateRegVx(opcode, getRegVx(opcode) xor getRegVy(opcode))

            0x0004 -> {
                var result = getRegVx(opcode) + getRegVy(opcode)
                V[0xF] = if (result > 0x00FF) 1 else 0
                updateRegVx(opcode, result and 0x00FF)
            }

            0x0005 -> {
                V[0xF] = if (getRegVx(opcode) > getRegVy(opcode)) 1 else 0
                updateRegVx(opcode, getRegVx(opcode) - getRegVy(opcode))
            }

            0x0006 -> {
                V[0xF] = getRegVx(opcode) and 0x1
                updateRegVx(opcode, getRegVx(opcode)  shr 1)
            }

            0x0007 -> {
                V[0xF] = if (getRegVy(opcode) > getRegVx(opcode)) 1 else 0
                updateRegVx(opcode, getRegVy(opcode) - getRegVx(opcode))
            }

            0x000E -> {
                V[0xF] = getRegVx(opcode) shr 7
                updateRegVx(opcode, getRegVx(opcode) shl 1)
            }

            else -> println("Unknown opcode [0x8000]: " + Integer.toHexString(opcode and 0x000F))
        }

    }


    fun display(): String {
        val builder = StringBuilder()
        builder.append(" " + "-".repeat(64) + "\n")
        for(y in 0..31) {
            builder.append("|")
            for (x in 0..63) {
                builder.append(if (gfx[x + y * 64] != 0)  "*" else " ")
            }
            builder.append("|\n")
        }
        builder.append(" " + "-".repeat(64) + "\n")
        return builder.toString()
    }

}

fun main(args: Array<String>) {
    val chip8 = Chip8()
    chip8.load_rom("roms/INVADERS")

    for(i in  1 until 300) {
        chip8.emulate()
    }

    println(chip8.display())
}