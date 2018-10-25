package com.ruimaranhao.chip8

import spock.lang.Specification

import com.ruimaranhao.chip8.emu.Memory

class Chip8Test extends Specification {

    def "load rom, returns true"() {
        setup:
        Memory memory = new Memory()

        when:
        Boolean result = memory.loadRom("roms/PONG")

        then:
        result == true
    }
}