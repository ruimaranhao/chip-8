import java.io.File
import java.util.*

const val MASK = 0xF000

class Chip8 {
    fun Byte.toPositiveInt() = toInt() and 0xFF

    fun IntRange.random() = Random().nextInt((endInclusive + 1) - start) +  start

    var pc: Int  = 0x200
    var opcode = 0

    var I = 0
    var sp = 0

    var delay_timer = 0
    var sound_timer = 0

    var V = Array(16) { _ -> 0}

    lateinit var memory: ByteArray

    fun load_rom(file : String): Boolean {
        var rom = File(file).readBytes()

        memory = ByteArray(512 + rom.size)
        System.arraycopy(rom, 0, memory, 512, rom.size)

        return true
    }


    fun emulate() {
        println("==========>>>> EMULATE")

        opcode = (memory[pc].toPositiveInt() shl 8) or memory[pc + 1].toPositiveInt()

        println(Integer.toString(pc) + "  " + Integer.toHexString(opcode))

        when(opcode and MASK) {
            0x6000 -> {
                V[(opcode and 0x0F00) shr 8] = opcode and 0x00FF
                pc += 2
            }

            0xA000 -> {
                I = opcode and 0x0FFF
                pc += 2
            }

            0xB000 -> {
                pc = (opcode and 0x0FFF) + V[0]
            }

            0xC000 -> {
                V[opcode and 0x0F00 shr 8] = (0..0xFF).random() % (0xFF + 1) and (opcode and 0x00FF)
                pc += 2
            }

            0xD000 -> {
                println("DRAWING SPRITE")

                pc += 2
            }

            0xE000 -> {
                process_EX(opcode)
            }

            0xF000 -> {
                process_FX(opcode)
            }

            else -> println("Main Opcode: " + Integer.toHexString(opcode and MASK))

        }

        println(Arrays.toString(V))
    }


    private fun process_FX(opcode: Int) {
        println("Processing FX: " + Integer.toHexString(opcode and 0x00FF))

        when(opcode and 0x00FF) {
            0x0007 -> {}
            0x000A -> {}
            0x0015 -> {}
            0x0018 -> {}
            0x001E -> {}
            0x0029 -> {}
            0x0033 -> {}
            0x0055 -> {}
            0x0065 -> {}
            else -> println("Opcode [0xFXAB]: " + Integer.toHexString(opcode and 0x00FF))

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


}

fun main(args: Array<String>) {
    val chip8 : Chip8 = Chip8()
    chip8.load_rom("roms/PONG")

    for(i in  1 until 100){
        chip8.emulate()
    }


}