package xyz.malefic.mfc.command.mic

import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.animation.textAnimation
import com.github.ajalt.mordant.rendering.TextColors.brightBlue
import com.github.ajalt.mordant.rendering.TextColors.brightCyan
import com.github.ajalt.mordant.rendering.TextColors.brightGreen
import com.github.ajalt.mordant.rendering.TextColors.brightMagenta
import com.github.ajalt.mordant.rendering.TextColors.brightRed
import com.github.ajalt.mordant.rendering.TextColors.brightYellow
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.magenta
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.info
import com.github.ajalt.mordant.terminal.success
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import xyz.malefic.mfc.util.CliktCommand
import xyz.malefic.mfc.util.interactiveBaseCommand
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.TargetDataLine
import kotlin.math.abs
import kotlin.math.sin

class MicCommand : CliktCommand("mic", "Manage the microphone and recordings") {
    init {
        subcommands(TestCommand())
    }

    override val invokeWithoutSubcommand = true

    override fun run() =
        Terminal().interactiveBaseCommand(
            this@MicCommand,
            buildMap {
                put("test", TestCommand())
            },
        )
}

class TestCommand : CliktCommand("test", "Test microphone with real-time audio visualizer") {
    private val duration by option("-d", "--duration", help = "Test duration in seconds")
        .int()
        .default(30)

    private val sensitivity by option("-s", "--sensitivity", help = "Visualizer sensitivity (1-10)")
        .int()
        .default(5)

    private val barHeight by option("-h", "--height", help = "Visualizer height in lines")
        .int()
        .default(8)

    override fun run() =
        with(Terminal()) {
            println(brightBlue("🎤 Advanced Microphone Test with Flow"))
            info("Starting flow-based audio visualizer... Press ${brightRed("Ctrl+C")} to stop")
            println()

            try {
                runBlocking {
                    startFlowBasedVisualization()
                }
            } catch (_: LineUnavailableException) {
                danger("The microphone is not available or is otherwise in use")
            } catch (e: Exception) {
                danger("❌ Error: ${e.message}")
            }
        }

    /**
     * Holds the state for the audio visualizer animation.
     *
     * @property currentHeight The current height of the visualizer bar.
     * @property targetHeight The target height the visualizer bar should animate to.
     * @property percentage The current audio level as a percentage.
     * @property frameCount The number of animation frames rendered.
     */
    data class AnimationState(
        val currentHeight: Int = 0,
        val targetHeight: Int = 0,
        val percentage: Int = 0,
        val frameCount: Long = 0,
    )

    private suspend fun Terminal.startFlowBasedVisualization() {
        val audioFlow = createAudioFlow()

        success("✅ Audio flow initialized successfully!")
        println()

        var animationState = AnimationState()

        val animation =
            textAnimation<AnimationState> { state ->
                generateVerticalVisualizer(state)
            }

        try {
            withTimeout(duration * 1000L) {
                audioFlow
                    .map { audioLevel ->
                        info("Processing audio level: $audioLevel")
                        val percentage = (audioLevel * 100).coerceIn(0.0, 100.0).toInt()
                        val targetHeight = ((audioLevel * barHeight).coerceIn(0.0, barHeight.toDouble())).toInt()
                        percentage to targetHeight
                    }.onEach { (percentage, targetHeight) ->
                        val newCurrentHeight =
                            smoothTransition(
                                animationState.currentHeight,
                                targetHeight,
                                0.3,
                            )

                        animationState =
                            animationState.copy(
                                currentHeight = newCurrentHeight,
                                targetHeight = targetHeight,
                                percentage = percentage,
                                frameCount = animationState.frameCount + 1,
                            )

                        animation.update(animationState)
                        delay(50)
                    }.collect()
            }
        } catch (_: TimeoutCancellationException) {
            // Normal timeout completion
        } catch (_: CancellationException) {
            // User cancellation
        } finally {
            animation.clear()
            success("\n✅ Audio visualization completed")
        }
    }

    private fun createAudioFlow(): Flow<Double> =
        flow {
            val format = AudioFormat(44100f, 16, 1, true, false)
            val info = DataLine.Info(TargetDataLine::class.java, format)

            check(AudioSystem.isLineSupported(info)) { "Audio format not supported" }

            val line = AudioSystem.getLine(info) as TargetDataLine
            line.open(format)
            line.start()

            try {
                val bufferSize = 4096
                val buffer = ByteArray(bufferSize)

                while (currentCoroutineContext().isActive) {
                    val bytesRead = line.read(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        val audioLevel = calculateAudioLevel(buffer, bytesRead)
                        emit(audioLevel)
                    }
                }
            } finally {
                line.stop()
                line.close()
            }
        }

    private fun calculateAudioLevel(
        buffer: ByteArray,
        bytesRead: Int,
    ): Double {
        var sum = 0.0
        var sampleCount = 0

        var i = 0
        while (i < bytesRead - 1) {
            val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort()
            sum += abs(sample.toDouble())
            sampleCount++
            i += 2
        }

        val average = if (sampleCount > 0) sum / sampleCount else 0.0
        return (average / Short.MAX_VALUE) * (sensitivity / 5.0)
    }

    private fun smoothTransition(
        current: Int,
        target: Int,
        factor: Double,
    ): Int {
        val difference = target - current
        val step = (difference * factor).toInt()
        return current + step
    }

    private fun generateVerticalVisualizer(state: AnimationState): String {
        val height = state.currentHeight
        val percentage = state.percentage
        val frame = state.frameCount

        val pulseIntensity = (sin(frame * 0.2) * 0.3 + 0.7)

        return buildString {
            appendLine(
                "${TextStyles.bold("Audio Level:")} ${getPercentageColor(percentage)("$percentage%")} ${getPeakIndicator(percentage)}",
            )
            appendLine()

            for (level in barHeight downTo 1) {
                append("${TextStyles.dim("│")} ")

                if (level <= height) {
                    val color = getHeightBasedColor(level, barHeight, pulseIntensity)
                    val block = if (level == height && pulseIntensity > 0.8) "█" else "▓"
                    append(color(block.repeat(getBarWidth(level, barHeight))))
                } else {
                    append(TextStyles.dim("░".repeat(getBarWidth(level, barHeight))))
                }

                append(" ${TextStyles.dim("│")}")

                when (level) {
                    barHeight -> append(" ${brightRed("HIGH")}")
                    barHeight * 3 / 4 -> append(" ${brightYellow("MID")}")
                    barHeight / 2 -> append(" ${brightGreen("LOW")}")
                    1 -> append(" ${TextStyles.dim("QUIET")}")
                }

                appendLine()
            }

            append("${TextStyles.dim("└")}${TextStyles.dim("─".repeat(getBarWidth(1, barHeight) + 2))}${TextStyles.dim("┘")}")
        }
    }

    private fun getHeightBasedColor(
        currentLevel: Int,
        maxHeight: Int,
        pulseIntensity: Double,
    ): (String) -> String {
        val ratio = currentLevel.toDouble() / maxHeight
        val isPulse = pulseIntensity > 0.8
        return {
            when {
                ratio > 0.8 -> if (isPulse) brightRed(it) else red(it)
                ratio > 0.6 -> if (isPulse) brightMagenta(it) else magenta(it)
                ratio > 0.4 -> if (isPulse) brightYellow(it) else yellow(it)
                ratio > 0.2 -> if (isPulse) brightGreen(it) else green(it)
                else -> if (isPulse) brightCyan(it) else cyan(it)
            }
        }
    }

    private fun getPercentageColor(percentage: Int): (String) -> String =
        {
            when {
                percentage > 80 -> brightRed(it)
                percentage > 60 -> brightYellow(it)
                percentage > 30 -> brightGreen(it)
                else -> brightCyan(it)
            }
        }

    private fun getPeakIndicator(percentage: Int): String =
        when {
            percentage > 90 -> brightRed("🔴 PEAK!")
            percentage > 70 -> brightYellow("🟡 HIGH")
            percentage > 30 -> brightGreen("🟢 GOOD")
            percentage > 10 -> brightCyan("🔵 ACTIVE")
            else -> TextStyles.dim("⚫ QUIET")
        }

    private fun getBarWidth(
        level: Int,
        maxHeight: Int,
    ): Int {
        val baseWidth = 30
        val extraWidth = (maxHeight - level) * 2
        return baseWidth + extraWidth
    }
}
