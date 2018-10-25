package com.ruimaranhao.chip8.display

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.IOException


/**
 * A class to emulate a Chip 8 Screen. The original Chip 8 screen was 64 x 32.
 * This class creates a simple AWT canvas with a single back buffer to store the
 * current state of the Chip 8 screen.
 *
 * Two colors are used on the Chip 8 - the `foreColor` and the `backColor`. The
 * former is used when turning pixels on, while the latter is used to turn pixels off.
 */
class Screen

@Throws(IOException::class)
@JvmOverloads constructor(// The current screen mode
        private var screenMode: ScreenMode? = ScreenMode(0, 0, normalScreenMode.scale)) {

    var buffer: BufferedImage? = null

    var stateChanged: Boolean = false

    private var foreColor = Color.white
    private var backColor = Color.black

    val height: Int
        get() = screenMode!!.height

    val width: Int
        get() = screenMode!!.width

    val scale: Int
        get() = screenMode!!.scale

    @Throws(IOException::class)
    constructor(scale: Int) : this(ScreenMode(0, 0, scale))

    init {
        setNormalScreenMode()
        stateChanged = false
    }

    /**
     * Generates the BufferedImage that will act as the back buffer for the
     * screen. Flags the Screen state as having changed.
     */
    private fun createBackBuffer() {
        buffer = BufferedImage(
                screenMode!!.width * screenMode!!.scale,
                screenMode!!.height * screenMode!!.scale,
                BufferedImage.TYPE_4BYTE_ABGR)

        stateChanged = true
    }

    /**
     * Low level routine to draw a pixel to the screen. Takes into account the
     * scaling factor applied to the screen. The top-left corner of the screen
     * is at coordinate (0, 0).
     *
     * @param x     The x coordinate to place the pixel
     * @param y     The y coordinate to place the pixel
     * @param color The Color of the pixel to draw
     */
    private fun drawPixelPrimitive(x: Int, y: Int, color: Color) {
        val scaleFactor = screenMode!!.scale
        val graphics = buffer!!.createGraphics()
        graphics.color = color
        graphics.fillRect(
                x * scaleFactor,
                y * scaleFactor, scaleFactor,
                scaleFactor)
        graphics.dispose()
    }

    fun pixelOn(x: Int, y: Int): Boolean {
        val scaleFactor = screenMode!!.scale
        val color = Color(
                buffer!!.getRGB(x * scaleFactor, y * scaleFactor),
                true)
        return color.equals(foreColor)
    }

    fun drawPixel(x: Int, y: Int, on: Boolean) {
        if (on) {
            drawPixelPrimitive(x, y, foreColor)
        } else {
            drawPixelPrimitive(x, y, backColor)
        }
    }

    /**
     * Clears the screen. Note that the caller must call
     * `updateScreen` to flush the back buffer to the screen.
     */
    fun clearScreen() {
        val scaleFactor = screenMode!!.scale
        val graphics = buffer!!.createGraphics()
        graphics.color = backColor
        graphics.fillRect(
                0,
                0,
                screenMode!!.width * scaleFactor,
                screenMode!!.height * scaleFactor)
        graphics.dispose()
    }

    fun scrollRight() {
        val scale = screenMode!!.scale
        val width = screenMode!!.width * scale
        val height = screenMode!!.height * scale
        val right = scale * 4

        val bufferedImage = copyImage(buffer!!.getSubimage(0, 0, width, height))
        val graphics = buffer!!.createGraphics()
        graphics.color = backColor
        graphics.fillRect(0, 0, width, height)
        graphics.drawImage(bufferedImage, right, 0, null)
        graphics.dispose()
    }

    /**
     * Scrolls the screen 4 pixels to the left.
     */
    fun scrollLeft() {
        val scale = screenMode!!.scale
        val width = screenMode!!.width * scale
        val height = screenMode!!.height * scale
        val left = -(scale * 4)

        val bufferedImage = copyImage(buffer!!.getSubimage(0, 0, width, height))
        val graphics = buffer!!.createGraphics()
        graphics.color = backColor
        graphics.fillRect(0, 0, width, height)
        graphics.drawImage(bufferedImage, left, 0, null)
        graphics.dispose()
    }

    /**
     * Scrolls the screen down by the specified number of pixels.
     *
     * @param numPixels the number of pixels to scroll down
     */
    fun scrollDown(numPixels: Int) {
        val scale = screenMode!!.scale
        val width = screenMode!!.width * scale
        val height = screenMode!!.height * scale
        val down = numPixels * scale

        val bufferedImage = copyImage(buffer!!.getSubimage(0, 0, width, height))
        val graphics = buffer!!.createGraphics()
        graphics.color = backColor
        graphics.fillRect(0, 0, width, height)
        graphics.drawImage(bufferedImage, 0, down, null)
        graphics.dispose()
    }

    /**
     * Turns on the extended screen mode for the emulator (when operating
     * in Super Chip 8 mode). Flags the state of the emulator screen as
     * having been changed.
     */
    fun setExtendedScreenMode() {
        this.screenMode = ScreenMode(
                extendedScreenMode.width,
                extendedScreenMode.height,
                this.screenMode!!.scale)
        createBackBuffer()
    }

    /**
     * Turns on the normal screen mode for the emulator (when operating
     * in Super Chip 8 mode).
     */
    fun setNormalScreenMode() {
        this.screenMode = ScreenMode(
                normalScreenMode.width,
                normalScreenMode.height,
                this.screenMode!!.scale)
        createBackBuffer()
    }

    /**
     * Clears the state change flag for the Screen.
     */
    fun clearStateChanged() {
        stateChanged = false
    }

    /**
     * Generates a copy of the original back buffer.
     *
     * @param source the source to copy from
     * @return a BufferedImage that is a copy of the original source
     */
    private fun copyImage(source: BufferedImage): BufferedImage {
        val bufferedImage = BufferedImage(source.width, source.height, source.type)
        val graphics = bufferedImage.graphics
        graphics.drawImage(source, 0, 0, null)
        graphics.dispose()
        return bufferedImage
    }

    companion object {
        // Screen dimensions for when the emulator is in normal mode
        var normalScreenMode = ScreenMode(64, 32, 14)

        // Screen dimensions for when the emulator is in extended mode
        private val extendedScreenMode = ScreenMode(128, 64, 7)
    }
}
