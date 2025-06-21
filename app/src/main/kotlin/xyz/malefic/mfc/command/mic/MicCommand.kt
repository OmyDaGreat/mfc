package xyz.malefic.mfc.command.mic

import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.animation.textAnimation
import com.github.ajalt.mordant.rendering.TextColors.brightBlue
import com.github.ajalt.mordant.rendering.TextColors.brightCyan
import com.github.ajalt.mordant.rendering.TextColors.brightGreen
import com.github.ajalt.mordant.rendering.TextColors.brightRed
import com.github.ajalt.mordant.rendering.TextColors.brightYellow
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.info
import com.github.ajalt.mordant.terminal.success
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

    override fun run() =
        runBlocking {
            with(Terminal()) {
                println(brightBlue("🎤 Microphone Test"))
                info("Starting audio visualizer... Press ${brightRed("q")} to quit")
                println()

                try {
                    startHorizontalVisualization()
                } catch (_: LineUnavailableException) {
                    danger("The microphone is not available or is otherwise in use")
                } catch (e: Exception) {
                    danger("❌ Error: ${e.message}")
                }
            }
        }

    private suspend fun Terminal.startHorizontalVisualization() {
        val audioFlow = createAudioFlow()
        success("✅ Audio flow initialized successfully!")
        println()

        val animation =
            textAnimation<Int> { percentage ->
                generateHorizontalVisualizer(percentage)
            }

        val job =
            CoroutineScope(Dispatchers.IO).launch {
                while (true) {
                    val char = System.`in`.read().toChar()
                    if (char == 'q' || char == 'Q') {
                        currentCoroutineContext().cancel()
                        break
                    }
                }
            }

        try {
            withTimeout(duration * 1000L) {
                audioFlow
                    .map { audioLevel ->
                        (audioLevel * 7000).coerceIn(0.0, 100.0).toInt()
                    }.onEach { percentage ->
                        animation.update(percentage)
                        delay(50)
                    }.collect()
            }
        } catch (_: TimeoutCancellationException) {
            // Normal timeout completion
        } catch (_: CancellationException) {
            // User cancellation
        } finally {
            job.cancel()
            animation.clear()
            success("\n✅ Audio visualization completed")
        }
    }

    private fun generateHorizontalVisualizer(percentage: Int): String {
        val maxBarLength = 50
        val barLength = (percentage * maxBarLength / 100)

        return buildString {
            appendLine(
                "${TextStyles.bold("Audio Level:")} ${percentage.getPercentageColor()("$percentage%")} ${percentage.getPeakIndicator()}",
            )
            append("${TextStyles.dim("[")} ")

            val lowSection = minOf(barLength, 20)
            if (lowSection > 0) {
                append(brightGreen("█".repeat(lowSection)))
            }

            val midSection = minOf(barLength - 20, 20).coerceAtLeast(0)
            if (midSection > 0) {
                append(brightYellow("█".repeat(midSection)))
            }

            val highSection = (barLength - 40).coerceAtLeast(0)
            if (highSection > 0) {
                append(brightRed("█".repeat(highSection)))
            }

            append(" ".repeat(maxBarLength - barLength))
            append(" ${TextStyles.dim("]")}")
        }
    }

    private fun Int.getPercentageColor(): (String) -> String =
        {
            when {
                this > 80 -> brightRed(it)
                this > 60 -> brightYellow(it)
                this > 30 -> brightGreen(it)
                else -> brightCyan(it)
            }
        }

    private fun Int.getPeakIndicator(): String =
        when {
            this > 90 -> brightRed("🔴 PEAK!")
            this > 70 -> brightYellow("🟡 HIGH")
            this > 30 -> brightGreen("🟢 GOOD")
            this > 10 -> brightCyan("🔵 ACTIVE")
            else -> TextStyles.dim("⚫ QUIET")
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
}
