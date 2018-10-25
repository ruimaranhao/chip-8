package com.ruimaranhao.chip8.emu

import com.ruimaranhao.chip8.display.Screen
import com.ruimaranhao.chip8.extensions.hex
import com.ruimaranhao.chip8.extensions.random
import java.util.Timer
import java.util.TimerTask
import java.util.logging.Logger
import javax.sound.midi.*

class CPU internal constructor(
        private val memory: Memory,
        private val keyboard: Keyboard,
        private val screen: Screen?) : Thread() {


    companion object {
        private val LOGGER = Logger.getLogger(EMU::class.java.name)

        private const val MODE_NORMAL = 1

        private const val MODE_EXTENDED = 2

        private const val DELAY_INTERVAL: Long = 17

        private const val NUM_REGISTERS = 16

        private const val PROGRAM_COUNTER_START = 0x200

        private const val DEFAULT_CPU_CYCLE_TIME: Long = 2
    }

    // The internal 8-bit registers
    private var v = ShortArray(16) { _ -> 0 }

    // The RPL register storage
    private var rpl: ShortArray = ShortArray(16) { _ -> 0 }

    // The index register
    private var index: Int = 0

    // The stack and stack pointer register
    private var sp: Int = 0
    private var stack = Array(16) { _ -> 0}

    // The program counter
    private var pc: Int = 0

    // The delay register
    private var delay: Short = 0

    // The sound register
    private var sound: Short = 0

    // The current operand
    private var operand: Int = 0

    // Determines if the CPU is paused
    private var paused: Boolean = false

    // Sets whether the CPU should remain alive or if it should die
    private var alive: Boolean = false

    // How long each instruction should take to execute (in milliseconds)
    private var cpuCycleTime: Long = 0

    // A Midi device for simple tone generation
    private var synthesizer: Synthesizer? = null

    // The Midi channel to perform playback on
    private var midiChannel: MidiChannel? = null

    // The current operating mode for the CPU
    private var mode: Int = 0


    init {
        paused = false
        alive = true
        val timer = Timer("Delay Timer")
        timer.schedule(object : TimerTask() {
            override fun run() {
                decrementTimers()
            }
        }, DELAY_INTERVAL, DELAY_INTERVAL)
        cpuCycleTime = DEFAULT_CPU_CYCLE_TIME

        try {
            synthesizer = MidiSystem.getSynthesizer()
            synthesizer!!.open()
            midiChannel = synthesizer!!.channels[0]
        } catch (e: MidiUnavailableException) {
            LOGGER.warning("Midi device not available for sound playback")
        }

        reset()
    }

    fun fetchIncrementExecute() {
        operand = (memory.read(pc).toInt() shl 8) or memory.read(pc + 1).toInt()
        pc += 2

        when ((operand and 0xF000) shr 12) {
            0x0 -> when (operand and 0x00FF) {
                0xE0 -> screen!!.clearScreen()

                0xEE -> returnFromSubroutine()

                0xFB -> scrollRight()

                0xFC -> scrollLeft()

                0xFD -> kill()

                0xFE -> disableExtendedMode()

                0xFF -> enableExtendedMode()

                else -> if (operand and 0xF0 == 0xC0) {
                    scrollDown(operand)
                } else {
                    LOGGER.info("0x0: Operation ${operand.hex} not supported")
                }
            }

            0x1 -> jumpToAddress()

            0x2 -> jumpToSubroutine()

            0x3 -> skipIfRegisterEqualValue()

            0x4 -> skipIfRegisterNotEqualValue()

            0x5 -> skipIfRegisterEqualRegister()

            0x6 -> moveValueToRegister()

            0x7 -> addValueToRegister()

            0x8 -> when (operand and 0x000F) {
                0x0 -> moveRegisterIntoRegister()

                0x1 -> logicalOr()

                0x2 -> logicalAnd()

                0x3 -> exclusiveOr()

                0x4 -> addRegisterToRegister()

                0x5 -> subtractRegisterFromRegister()

                0x6 -> rightShift()

                0x7 -> subtractRegisterFromRegister1()

                0xE -> leftShift()

                else -> LOGGER.info("Operation ${operand.hex} not supported")
            }

            0x9 -> skipIfRegisterNotEqualRegister()

            0xA -> loadIndexWithValue()

            0xB -> jumpToIndexPlusValue()

            0xC -> generateRandomNumber()

            0xD -> drawSprite()

            0xE -> when (operand and 0x00FF) {
                0x9E -> skipIfKeyPressed()

                0xA1 -> skipIfKeyNotPressed()

                else -> LOGGER.info("Operation ${operand.hex} not supported")
            }

            0xF -> when (operand and 0x00FF) {
                0x07 -> moveDelayTimerIntoRegister()

                0x0A -> waitForKeypress()

                0x15 -> moveRegisterIntoDelayRegister()

                0x18 -> moveRegisterIntoSoundRegister()

                0x1E -> addRegisterIntoIndex()

                0x29 -> loadIndexWithSprite()

                0x30 -> loadIndexWithExtendedSprite()

                0x33 -> storeBCDInMemory()

                0x55 -> storeRegistersInMemory()

                0x65 -> readRegistersFromMemory()

                0x75 -> storeRegistersInRPL()

                0x85 -> readRegistersFromRPL()

                else -> LOGGER.info("Operation ${operand.hex} not supported")
            }

            else -> LOGGER.info("Operation ${operand.hex} not supported")
        }
    }

    private fun returnFromSubroutine() {
        pc = stack[--sp]
    }

    private fun jumpToAddress() {
        pc = operand and 0x0FFF
    }

    private fun jumpToSubroutine() {
        stack[sp++] = pc
        jumpToAddress()
    }

    private fun skipIfRegisterEqualValue() {
        val sourceRegister = operand and 0x0F00 shr 8
        if (v[sourceRegister].toInt() == operand and 0x00FF) {
            pc += 2
        }
    }

    private fun skipIfRegisterNotEqualValue() {
        val sourceRegister = operand and 0x0F00 shr 8
        if (v[sourceRegister].toInt() != operand and 0x00FF) {
            pc += 2
        }
    }

    private fun skipIfRegisterEqualRegister() {
        val sourceRegister = operand and 0x0F00 shr 8
        val targetRegister = operand and 0x00F0 shr 4
        if (v[sourceRegister] == v[targetRegister]) {
            pc += 2
        }
    }

    private fun moveValueToRegister() {
        val targetRegister = operand and 0x0F00 shr 8
        v[targetRegister] = (operand and 0x00FF).toShort()
    }

    private fun addValueToRegister() {
        val targetRegister = operand and 0x0F00 shr 8
        val temp = v[targetRegister] + (operand and 0x00FF)
        v[targetRegister] = if (temp < 256) temp.toShort() else (temp - 256).toShort()
    }

    private fun moveRegisterIntoRegister() {
        val targetRegister = operand and 0x0F00 shr 8
        val sourceRegister = operand and 0x00F0 shr 4
        v[targetRegister] = v[sourceRegister]
    }

    private fun logicalOr() {
        val targetRegister = operand and 0x0F00 shr 8
        val sourceRegister = operand and 0x00F0 shr 4
        v[targetRegister] = (v[targetRegister].toInt() or v[sourceRegister].toInt()).toShort()
    }

    private fun logicalAnd() {
        val targetRegister = operand and 0x0F00 shr 8
        val sourceRegister = operand and 0x00F0 shr 4
        v[targetRegister] = (v[targetRegister].toInt() and v[sourceRegister].toInt()).toShort()
    }

    private fun exclusiveOr() {
        val targetRegister = operand and 0x0F00 shr 8
        val sourceRegister = operand and 0x00F0 shr 4
        v[targetRegister] = (v[targetRegister].toInt() xor v[sourceRegister].toInt()).toShort()
    }

    private fun addRegisterToRegister() {
        val targetRegister = operand and 0x0F00 shr 8
        val sourceRegister = operand and 0x00F0 shr 4
        val temp = v[targetRegister] + v[sourceRegister]
        if (temp > 255) {
            v[targetRegister] = (temp - 256).toShort()
            v[0xF] = 1
        } else {
            v[targetRegister] = temp.toShort()
            v[0xF] = 0
        }
    }

    private fun subtractRegisterFromRegister() {
        val targetRegister = operand and 0x0F00 shr 8
        val sourceRegister = operand and 0x00F0 shr 4
        val resultValue: Int
        if (v[targetRegister] > v[sourceRegister]) {
            resultValue = v[targetRegister] - v[sourceRegister]
            v[0xF] = 1
        } else {
            resultValue = 256 + v[targetRegister] - v[sourceRegister]
            v[0xF] = 0
        }
        v[targetRegister] = resultValue.toShort()
    }

    private fun rightShift() {
        val sourceRegister = operand and 0x0F00 shr 8
        v[0xF] = (v[sourceRegister].toInt() and 0x1).toShort()
        v[sourceRegister] = (v[sourceRegister].toInt() shr 1).toShort()
    }

    private fun subtractRegisterFromRegister1() {
        val targetRegister = operand and 0x0F00 shr 8
        val sourceRegister = operand and 0x00F0 shr 4
        val resultValue: Int
        if (v[sourceRegister] > v[targetRegister]) {
            resultValue = v[sourceRegister] - v[targetRegister]
            v[0xF] = 1
        } else {
            resultValue = 256 + v[sourceRegister] - v[targetRegister]
            v[0xF] = 0
        }
        v[targetRegister] = resultValue.toShort()
    }

    /**
     * Shift the bits in the specified register 1 bit to the left. Bit 7 will be
     * shifted into register VF.
     */
    private fun leftShift() {
        val sourceRegister = operand and 0x0F00 shr 8
        v[0xF] = (v[sourceRegister].toInt() and 0x80 shr 8).toShort()
        v[sourceRegister] = (v[sourceRegister].toInt() shl 1).toShort()
    }

    private fun skipIfRegisterNotEqualRegister() {
        val sourceRegister = operand and 0x0F00 shr 8
        val targetRegister = operand and 0x00F0 shr 4
        if (v[sourceRegister] != v[targetRegister]) {
            pc += 2
        }
    }

    private fun loadIndexWithValue() {
        index = (operand and 0x0FFF).toShort().toInt()
    }

    private fun jumpToIndexPlusValue() {
        pc = index + (operand and 0x0FFF)
    }

    private fun generateRandomNumber() {
        val value = operand and 0x00FF
        val targetRegister = operand and 0x0F00 shr 8
        v[targetRegister] = (value and (0..0xFF).random() % (0xFF + 1)).toShort()
    }

    private fun drawSprite() {
        val xRegister = operand and 0x0F00 shr 8
        val yRegister = operand and 0x00F0 shr 4
        val xPos = v[xRegister].toInt()
        val yPos = v[yRegister].toInt()
        val numBytes = operand and 0xF
        v[0xF] = 0

        if (mode == MODE_EXTENDED && numBytes == 0) {
            drawExtendedSprite(xPos, yPos)
        } else {
            drawNormalSprite(xPos, yPos, numBytes)
        }
    }

    private fun drawExtendedSprite(xPos: Int, yPos: Int) {
        for (yIndex in 0..15) {
            for (xByte in 0..1) {
                val colorByte = memory.read(index + yIndex * 2 + xByte)
                var yCoord = yPos + yIndex
                yCoord %= screen!!.height

                var mask = 0x80

                for (xIndex in 0..7) {
                    var xCoord = xPos + xIndex + xByte * 8
                    xCoord %= screen.width

                    var turnOn = colorByte.toInt() and mask > 0
                    val currentOn = screen.pixelOn(xCoord, yCoord)

                    if (turnOn && currentOn) {
                        v[0xF] = (v[0xF].toInt() or 1).toShort()
                        turnOn = false
                    } else if (!turnOn && currentOn) {
                        turnOn = true
                    }

                    screen.drawPixel(xCoord, yCoord, turnOn)
                    mask = mask shr 1
                }
            }
        }
    }

    private fun drawNormalSprite(xPos: Int, yPos: Int, numBytes: Int) {
        for (yIndex in 0 until numBytes) {
            val colorByte = memory.read(index + yIndex)
            var yCoord = yPos + yIndex
            yCoord %= screen!!.height

            var mask = 0x80

            for (xIndex in 0..7) {
                var xCoord = xPos + xIndex
                xCoord %= screen.width

                var turnOn = colorByte.toInt() and mask > 0
                val currentOn = screen.pixelOn(xCoord, yCoord)

                if (turnOn && currentOn) {
                    v[0xF] = (v[0xF].toInt() or 1).toShort()
                    turnOn = false
                } else if (!turnOn && currentOn) {
                    turnOn = true
                }

                screen.drawPixel(xCoord, yCoord, turnOn)
                mask = mask shr 1
            }
        }
    }

    private fun skipIfKeyPressed() {
        val sourceRegister = operand and 0x0F00 shr 8
        val keyToCheck = v[sourceRegister].toInt()
        if (keyboard.currentKey == keyToCheck) {
            pc += 2
        }
    }

    private fun skipIfKeyNotPressed() {
        val sourceRegister = operand and 0x0F00 shr 8
        val keyToCheck = v[sourceRegister].toInt()
        if (keyboard.currentKey != keyToCheck) {
            pc += 2
        }
    }

    private fun moveDelayTimerIntoRegister() {
        val targetRegister = operand and 0x0F00 shr 8
        v[targetRegister] = delay
    }

    private fun waitForKeypress() {
        val targetRegister = operand and 0x0F00 shr 8
        var currentKey = keyboard.currentKey
        println(currentKey)
        while (currentKey == 0) {
            try {
                Thread.sleep(300)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            currentKey = keyboard.currentKey
        }
        v[targetRegister] = currentKey.toShort()
    }

    private fun moveRegisterIntoDelayRegister() {
        val sourceRegister = operand and 0x0F00 shr 8
        delay = v[sourceRegister]
    }

    private fun moveRegisterIntoSoundRegister() {
        val sourceRegister = operand and 0x0F00 shr 8
        sound = v[sourceRegister]
    }

    private fun loadIndexWithSprite() {
        val sourceRegister = operand and 0x0F00 shr 8
        index = v[sourceRegister] * 5
    }

    private fun loadIndexWithExtendedSprite() {
        val sourceRegister = operand and 0x0F00 shr 8
        index = v[sourceRegister] * 10
    }

    private fun addRegisterIntoIndex() {
        val sourceRegister = operand and 0x0F00 shr 8
        index += v[sourceRegister].toInt()
    }

    private fun storeBCDInMemory() {
        val sourceRegister = operand and 0x0F00 shr 8
        val bcdValue = v[sourceRegister].toInt()
        memory.write(bcdValue / 100, index)
        memory.write(bcdValue % 100 / 10, index + 1)
        memory.write(bcdValue % 100 % 10, index + 2)
    }

    private fun storeRegistersInMemory() {
        val numRegisters = operand and 0x0F00 shr 8
        for (counter in 0..numRegisters) {
            memory.write(v[counter].toInt(), index + counter)
        }
    }

    private fun readRegistersFromMemory() {
        val numRegisters = operand and 0x0F00 shr 8
        for (counter in 0..numRegisters) {
            v[counter] = memory.read(index + counter)
        }
    }

    fun reset() {
        v = ShortArray(NUM_REGISTERS) { _ -> 0}
        rpl = ShortArray(NUM_REGISTERS) { _ -> 0}
        pc = PROGRAM_COUNTER_START
        sp = 0
        stack = Array(NUM_REGISTERS) { _ -> 0}
        index = 0
        delay = 0
        sound = 0
        screen?.clearScreen()
    }

    private fun decrementTimers() {
        if (delay.toInt() != 0) {
            delay--
        }
        if (sound.toInt() != 0) {
            sound--
            midiChannel!!.noteOn(60, 50)
        }
        if (sound.toInt() == 0 && midiChannel != null) {
            midiChannel!!.noteOff(60)
        }
    }

    private fun enableExtendedMode() {
        screen!!.setExtendedScreenMode()
        mode = MODE_EXTENDED
    }

    private fun disableExtendedMode() {
        screen!!.setNormalScreenMode()
        mode = MODE_NORMAL
    }

    private fun scrollLeft() {
        screen!!.scrollLeft()
    }

    private fun scrollRight() {
        screen!!.scrollRight()
    }

    private fun storeRegistersInRPL() {
        val numRegisters = operand and 0x0F00 shr 8
        for (counter in 0..numRegisters) {
            rpl[counter] = v[counter]
        }
    }

    private fun readRegistersFromRPL() {
        val numRegisters = operand and 0x0F00 shr 8
        for (counter in 0..numRegisters) {
            v[counter] = rpl[counter]
        }
    }

    private fun scrollDown(operand: Int) {
        val numPixels = operand and 0xF
        screen!!.scrollDown(numPixels)
    }

    fun setPaused(paused: Boolean) {
        this.paused = paused
    }

    fun setCPUCycleTime(cycleTime: Long) {
        cpuCycleTime = cycleTime
    }

    override fun run() {
        while (alive) {
            if (!paused) {
                fetchIncrementExecute()
                try {
                    Thread.sleep(cpuCycleTime)
                } catch (e: InterruptedException) {
                    LOGGER.warning("CPU sleep interrupted")
                }

            } else {
                try {
                    Thread.sleep(300)
                } catch (e: InterruptedException) {
                    LOGGER.warning("Pause interrupted")
                }

            }
        }
    }

    private fun kill() {
        alive = false
        synthesizer!!.close()
    }

    fun trace(): String {
        return "I: ${index.hex} DT: ${delay.hex} ST: ${sound.hex} PC: ${pc.hex}"

    }
}