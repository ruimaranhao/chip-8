import java.io.File
import java.util.*

const val MASK = 0xF000

class Chip8 {
    fun Byte.toPositiveInt() = toInt() and 0xFF
    fun IntRange.random() = Random().nextInt((endInclusive + 1) - start) +  start
    val Int.hex: String get() = Integer.toHexString(this)
    val Byte.hex: String get() = Integer.toHexString(this.toInt())

    val fontData = intArrayOf(
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

    var delay_timer = 0
    var sound_timer = 0

    var stack = Array(16) { _ -> 0}
    var key = Array(16) { _ -> 0}
    var V = Array(16) { _ -> 0}
    var gfx = Array(64 * 32) { _ -> 0}

    val display: ByteArray = ByteArray(64*32)

    lateinit var memory: ByteArray

    var debug = StringBuilder("")


    fun load_rom(file : String): Boolean {
        var rom = File(file).readBytes()

        memory = ByteArray(512 + rom.size)

        for (i in 0 until fontData.size) {
            memory[i] = fontData[i].toByte()
        }

        System.arraycopy(rom, 0, memory, 512, rom.size)

        return true
    }


    fun emulate() {
        println("==========>>>> EMULATE")

        opcode = (memory[pc].toPositiveInt() shl 8) or memory[pc + 1].toPositiveInt()

        println(Integer.toString(pc) + "  " + Integer.toHexString(opcode))
        when(opcode and MASK) {
            0x0000 -> process_00E(opcode)

            0x1000 -> pc = opcode and 0x0FFF

            0x2000 -> {
                stack[sp++] = pc
                pc = opcode and 0x0FFF
            }

            0x3000 -> pc += if (V[opcode and 0x0F00 shr 8] === opcode and 0x00FF) 4 else 2

            0x4000 -> pc += if (V[opcode and 0x0F00 shr 8] !== opcode and 0x00FF) 4 else 2

            0x5000 -> pc += if (V[opcode and 0x0F00 shr 8] === V[opcode and 0x00F0 shr 4]) 4 else 2

            0x6000 -> {
                val reg = opcode and 0x0F00 shr 8
                val value = opcode and 0x00FF

                V[reg] = value
                pc += 2

                debug.append("set v${reg.hex}, 0x${value.hex}\n")
            }

            0x7000 -> {
                V[opcode and 0x0F00 shr 8] += opcode and 0x00F
                pc += 2
            }


            0x9000 -> pc += if (V[opcode and 0x0F00 shr 8] !== V[opcode and 0x00F0 shr 4]) 4 else 2

            0xA000 -> {
                I = opcode and 0x0FFF
                pc += 2
            }

            0xB000 -> pc = (opcode and 0x0FFF) + V[0]

            0xC000 -> {
                V[opcode and 0x0F00 shr 8] = (0..0xFF).random() % (0xFF + 1) and (opcode and 0x00FF)
                pc += 2
            }

            0xD000 -> {
                val x = V[opcode and 0x0F00 shr 8]
                val y = V[opcode and 0x00F0 shr 4]
                val height = opcode and 0x000F
                var pixel : Byte

                V[0xF] = 0

                for(yline in 0 until height) {
                    pixel = memory[I + yline]

                    for(xline in 0..8) {
                        if (pixel.toPositiveInt() and (0x80 shr xline) != 0) {
                            if (gfx[x + xline + (y + yline) * 64] === 1) {
                                V[0xF] = 1
                            }
                            gfx[x + xline + (y + yline) * 64] = gfx[x + xline + (y + yline) * 64] xor 1
                        }
                    }
                }

                pc += 2
            }

            0xE000 -> process_EX(opcode)


            0xF000 -> process_FX(opcode)


            else -> println("Unknown Main Opcode: " + Integer.toHexString(opcode and MASK))


        }

    }

    private fun process_00E(opcode: Int) {
        println(Integer.toHexString((opcode and 0x000F)))

        when (opcode and 0x000F) {
            0x0000 -> {
                println("Clear screen")
                for (i in 0..2047) {
                    gfx[i] = 0
                }
                pc += 2
            }

            0x000E -> {
                println("Return")

                --sp
                pc = stack[sp]
                pc += 2
            }

            else -> println("\nUnknown opcode [0x00E0]: ${opcode}\n")

        }
    }


    private fun process_FX(opcode: Int) {
        println("Processing FX: " + Integer.toHexString(opcode and 0x00FF))

        when(opcode and 0x00FF) {
            0x0007 -> {
                V[opcode and 0x0F00 shr 8] = delay_timer
                pc += 2
            }
            0x000A -> {
                var key_pressed = false

                for (i in 0..15) {
                    if (key[i] !== 0) {
                        V[opcode and 0x0F00 shr 8] = i
                        key_pressed = true
                    }
                }

                if(!key_pressed)
                    return

                pc += 2
            }
            0x0015 -> {
                delay_timer = V[opcode and 0x0F00 shr 8]
                pc += 2
            }
            0x0018 -> {
                sound_timer = V[opcode and 0x0F00 shr 8]
                pc += 2
            }
            0x001E -> {
                V[0xF] = if (I + V[opcode and 0x0F00 shr 8] > 0xFFF) 1 else 0
                I += V[opcode and 0x0F00 shr 8]
                pc += 2
            }
            0x0029 -> {
                I = V[opcode and 0x0F00 shr 8] * 0x5
                println("Sprite: " + I)
                pc += 2
            }
            0x0033 -> {
                memory[I] = (V[opcode and 0x0F00 shr 8] / 100).toByte()
                memory[I + 1] = (V[opcode and 0x0F00 shr 8] / 10 % 10).toByte()
                memory[I + 2] = (V[opcode and 0x0F00 shr 8] % 100 % 10).toByte()
                pc += 2
            }
            0x0055 -> {
                for (i in 0..(opcode and 0x0F00 shr 8))
                    memory[I + i] = V[i].toByte()

                I += (opcode and 0x0F00 shr 8) + 1
                pc += 2
            }
            0x0065 -> {
                for (i in 0..(opcode and 0x0F00 shr 8))
                    V[i] = memory[I + i].toPositiveInt()

                I += (opcode and 0x0F00 shr 8) + 1
                pc += 2

            }
            else -> println("Unknown opcode [0xFXAB]: " + Integer.toHexString(opcode and 0x00FF))

        }
    }

    private fun process_EX(opcode: Int) {
        println("Processing EX: " + Integer.toHexString(opcode and 0x00FF))

        when(opcode and 0x00FF) {
            0x009E -> {
                pc += 4
            }

            0x00A1 -> {
                pc += 4
            }
        }

    }


    fun display(): String {
        val builder = StringBuilder()
        builder.append("-".repeat(64) + "\n")
        for(y in 0..31) {
            builder.append("|")
            for (x in 0..63) {
                builder.append(if (gfx[x + y * 64] != 0)  "*" else " ")
            }
            builder.append("|\n")
        }
        builder.append("-".repeat(64) + "\n")
        return builder.toString()
    }

}

fun main(args: Array<String>) {
    val chip8 = Chip8()
    chip8.load_rom("roms/PONG")

    for(i in  1 until 100){
        chip8.emulate()
    }

    println(chip8.display())


}