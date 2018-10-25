package com.ruimaranhao.chip8

import spock.lang.Specification

class Chip8Test extends Specification {

    def "load rom, returns true"() {
        setup:
        val memory = Memory()

        when:
        val result = memory.loadRom("roms/PONG")

        then:
        result == true
    }
}