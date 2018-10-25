package com.ruimaranhao.chip8.emu

import com.ruimaranhao.chip8.display.*
import javax.swing.JMenuBar
import javax.swing.JCheckBoxMenuItem
import javax.swing.JFrame
import java.awt.image.BufferedImage
import javax.swing.WindowConstants
import javax.swing.JPanel
import java.awt.*
import javax.swing.JMenuItem
import javax.swing.JMenu
import javax.swing.JOptionPane
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import java.awt.event.KeyEvent
import java.util.*
import java.awt.Graphics2D
import java.awt.AlphaComposite
import java.awt.Dimension
import java.io.File
import java.util.TimerTask
import java.util.logging.Logger


class EMU

@JvmOverloads constructor(screenMode: ScreenMode = Screen.normalScreenMode,
                          cycleTime: Long = 0,
                          rom: String? = null,
                          private var isInTraceMode: Boolean = false) {

    companion object {
        private val LOGGER = Logger.getLogger(EMU::class.java.name)

        private const val DEFAULT_NUMBER_OF_BUFFERS = 2

        private const val DEFAULT_TITLE = "Yet Another Chip 8 Emulator, this one in Kotlin"

        private const val DEFAULT_FONT = "VeraMono.ttf"

        private val overlayBackColor = Color(0.0f, 0.27f, 0.0f, 1.0f)

        private val overlayBorderColor = Color(0.0f, 0.70f, 0.0f, 1.0f)
    }

    private val cpu: CPU

    private var screen: Screen? = null

    private val keyboard: Keyboard = Keyboard()

    private val memory = Memory()

    private var overlayScreen: BufferedImage? = null

    private var overlayFont: Font? = null

    private var canvas: Canvas? = null

    private var container: JFrame? = null

    var step: Boolean = false
        set(step) {
            field = step
            mStepMenuItem!!.state = step
            if (step) {
                trace = true
            }
            cpu!!.setPaused(step)
        }

    private var traceMenuItem: JCheckBoxMenuItem? = null

    private var mStepMenuItem: JCheckBoxMenuItem? = null

    private var menuBar: JMenuBar? = null

    var trace: Boolean
        get() = isInTraceMode
        set(trace) {
            isInTraceMode = trace
            traceMenuItem!!.state = trace
        }

    init {
        try {
            screen = Screen(screenMode)
        } catch (e: Exception) {
            LOGGER.severe("Could not initialize screen")
            LOGGER.severe(e.message)
            System.exit(1)
        }

        // Initialize the CPU
        cpu = CPU(memory, keyboard, screen)
        cpu.setCPUCycleTime(cycleTime)

        // Attempt to load specified ROM file
        if (rom != null) {
            if (!memory.loadRom(rom)) {
                LOGGER.severe("Could not load ROM [$rom]")
            }
        } else {
            cpu.setPaused(true)
        }

        initEmulatorJFrame()
        initializeOverlay()
    }

    /**
     * Starts the main emulator loop running. Fires at the rate of 60Hz,
     * will repaint the screen and listen for any debug key presses.
     */
    fun start() {
        cpu.start()
        val timer = Timer()
        val task = object : TimerTask() {
            override fun run() {
                refreshScreen()
                interpretDebugKey()
            }
        }
        timer.scheduleAtFixedRate(task, 0L, 33L)
    }

    private fun initializeOverlay() {
        val classLoader = javaClass.classLoader
        try {
            val fontFile = classLoader.getResourceAsStream(DEFAULT_FONT)

            overlayFont = Font.createFont(Font.TRUETYPE_FONT, fontFile)
            overlayFont = overlayFont!!.deriveFont(11f)
            overlayScreen = BufferedImage(342, 53, BufferedImage.TYPE_4BYTE_ABGR)

            fontFile.close()

        } catch (e: Exception) {
            e.printStackTrace()
            LOGGER.severe("Could not initialize overlayScreen ")
            LOGGER.severe(e.localizedMessage)
            System.exit(1)
        }

    }


    fun loadFile() {
        val fileChooser = JFileChooser()
        val filter1 = FileNameExtensionFilter("CHIP8 Rom File (*.chip8)", "chip8")
        fileChooser.currentDirectory = File(".")
        fileChooser.dialogTitle = "Open ROM file"
        fileChooser.isAcceptAllFileFilterUsed = true
        fileChooser.fileFilter = filter1
        if (fileChooser.showOpenDialog(container) == JFileChooser.APPROVE_OPTION) {
            cpu.setPaused(true)
            val romFile = fileChooser.selectedFile.toString()
            if (!memory.loadRom(romFile)) {
                JOptionPane.showMessageDialog(container, "Error reading file.", "File Read Problem",
                        JOptionPane.ERROR_MESSAGE)
                return
            }
            cpu.reset()
            cpu.setPaused(false)
        }
    }

    /**
     * Initializes the JFrame that the emulator will use to draw onto. Will set up the menu system and
     * link the action listeners to the menu items. Returns the JFrame that contains all of the emulator
     * screen elements.
     */
    private fun initEmulatorJFrame() {
        container = JFrame(DEFAULT_TITLE)
        menuBar = JMenuBar()

        // File menu
        val fileMenu = JMenu("File")
        fileMenu.mnemonic = KeyEvent.VK_F

        val openFile = JMenuItem("Open", KeyEvent.VK_O)
        openFile.addActionListener(OpenMenuItemActionListener(this))
        fileMenu.add(openFile)
        fileMenu.addSeparator()

        val quitFile = JMenuItem("Quit", KeyEvent.VK_Q)
        quitFile.addActionListener(QuitMenuItemActionListener())
        fileMenu.add(quitFile)
        menuBar!!.add(fileMenu)

        // CPU menu
        val debugMenu = JMenu("CPU")
        debugMenu.mnemonic = KeyEvent.VK_C

        // Reset CPU menu item
        val resetCPU = JMenuItem("Reset", KeyEvent.VK_R)
        //resetCPU.addActionListener(ResetMenuItemActionListener(cpu))
        debugMenu.add(resetCPU)
        debugMenu.addSeparator()

        // Trace menu item
        traceMenuItem = JCheckBoxMenuItem("Trace Mode")
        traceMenuItem!!.mnemonic = KeyEvent.VK_T
        traceMenuItem!!.addItemListener(TraceMenuItemListener(this))
        debugMenu.add(traceMenuItem)

        // Step menu item
        mStepMenuItem = JCheckBoxMenuItem("Step Mode")
        mStepMenuItem!!.mnemonic = KeyEvent.VK_S
        mStepMenuItem!!.addItemListener(StepMenuItemListener(this))
        debugMenu.add(mStepMenuItem)
        menuBar!!.add(debugMenu)

        attachCanvas()
    }

    private fun attachCanvas() {
        val scaleFactor = screen!!.scale
        val scaledWidth = screen!!.width * scaleFactor
        val scaledHeight = screen!!.height * scaleFactor

        val panel = container!!.contentPane as JPanel
        panel.removeAll()
        panel.preferredSize = Dimension(scaledWidth, scaledHeight)
        panel.layout = null

        canvas = Canvas()
        canvas!!.setBounds(0, 0, scaledWidth, scaledHeight)
        canvas!!.ignoreRepaint = true

        panel.add(canvas)

        container!!.jMenuBar = menuBar
        container!!.pack()
        container!!.isResizable = false
        container!!.isVisible = true
        container!!.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        canvas!!.createBufferStrategy(DEFAULT_NUMBER_OF_BUFFERS)
        canvas!!.isFocusable = true
        canvas!!.requestFocus()

        canvas!!.addKeyListener(keyboard)
    }

    /**
     * Will redraw the contents of the screen to the emulator window. Optionally, if
     * isInTraceMode is True, will also draw the contents of the overlayScreen to the screen.
     */
    private fun refreshScreen() {

        // Check to see if the canvas should be regenerated
        if (screen!!.stateChanged) {
            attachCanvas()
            screen!!.clearStateChanged()
            screen!!.clearScreen()
        }

        val graphics = canvas!!.bufferStrategy.drawGraphics as Graphics2D
        graphics.drawImage(screen!!.buffer, null, 0, 0)

        // If in trace mode, then draw the trace overlay
        if (isInTraceMode) {
            updateOverlayInformation()
            val composite = AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 0.7f)
            graphics.composite = composite
            graphics.drawImage(overlayScreen, null, 5, screen!!.height * screen!!.scale - 57)
        }
        graphics.dispose()
        canvas!!.bufferStrategy.show()
    }

    private fun updateOverlayInformation() {
        val graphics = overlayScreen!!.createGraphics()

        graphics.color = overlayBorderColor
        graphics.fillRect(0, 0, 342, 53)

        graphics.color = overlayBackColor
        graphics.fillRect(1, 1, 340, 51)

        graphics.color = Color.white
        graphics.font = overlayFont

        val line = cpu.trace()

        graphics.drawString(line, 5, 16)

        graphics.dispose()
    }

    fun dispose() {
        container!!.dispose()
    }

    private fun interpretDebugKey() {
        val key = keyboard.debugKey
        when (key) {
            Keyboard.CHIP8_NORMAL -> {
                trace = false
                step = false
            }

            Keyboard.CHIP8_STEP -> step = !step

            Keyboard.CHIP8_TRACE -> trace = !isInTraceMode

            Keyboard.CHIP8_NEXT -> cpu.fetchIncrementExecute()

            else -> { }
        }
    }
}