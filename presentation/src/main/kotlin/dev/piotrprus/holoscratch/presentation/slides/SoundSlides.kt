package dev.piotrprus.holoscratch.presentation.slides

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.piotrprus.holoscratch.presentation.deck.Slide
import dev.piotrprus.holoscratch.presentation.demos.DustGate
import dev.piotrprus.holoscratch.presentation.demos.ScratchConfig
import dev.piotrprus.holoscratch.presentation.demos.ScratchPanel
import dev.piotrprus.holoscratch.presentation.demos.ScratchState
import dev.piotrprus.holoscratch.presentation.demos.SoundBench
import dev.piotrprus.holoscratch.presentation.ui.DeckColors
import dev.piotrprus.holoscratch.presentation.ui.MorphingCodeBlock

// Chapter 6 — "It sounds like foil". One recording, a first try that felt wrong, a look at the
// waveform to see why, and the fix: short grains, one per bit of finger travel.

private const val TRIM_START = 0.18f
private const val GRAIN = 0.125f
private const val GRAINS = 16

private val GRAIN_CODE = """
travel += distance(position, last)
if (fresh > 0.05f && travel >= 36.dp) {
    travel = 0f
    grains.random().play(volume = fresh)
}
"""

private val LOOP_CODE = """
// first try: one long loop
if (fresh > 0.05f) clip.loop() else clip.stop()
clip.volume = fresh
"""

/**
 * Steps: 0 the recording · 1 first try: loop it while dust comes out, and what went wrong ·
 * 2 why it started late: the first 400 ms · 3 trim and cut into grains · 4 one grain per bit of
 * travel, live.
 */
val soundSlide = Slide(steps = 4) {
    val (title, subtitle) = when (step) {
        0 -> "One recording" to "2.6 s of foil scraped with a coin"
        1 -> "First try" to "loop it while dust comes out"
        2 -> "Why late?" to "the first 400 ms of the file"
        3 -> "Trim and cut" to "16 grains of 120 ms"
        else -> "One grain per scrape" to "the sound follows the finger"
    }
    LiveSlide(title = title, subtitle = subtitle) { tilt ->
        val (peaks, seconds) = SoundBench.originalPeaks
        Row(horizontalArrangement = Arrangement.spacedBy(50.dp)) {
            Column(Modifier.weight(1.2f)) {
                Waveform(
                    peaks = peaks,
                    seconds = seconds,
                    window = if (step == 2) 0f to 0.4f else 0f to seconds,
                    showLeadIn = step == 2,
                    showGrains = step >= 3,
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                )
                Gap(16.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    ChipButton("▶  whole file", SoundBench::playOriginal)
                    if (step >= 3) ChipButton("▶  one grain", SoundBench::playGrain)
                }
                Gap(30.dp)
                when (step) {
                    0 -> Caption("I recorded a real scratch card.\nThe sound should come with the dust.", color = DeckColors.textPrimary)
                    1 -> Column {
                        MorphingCodeBlock(code = LOOP_CODE, fontSize = 22.sp, modifier = Modifier.fillMaxWidth())
                        Gap(20.dp)
                        Caption("It started late.\nAnd 2 s of scraping didn't match a short flick.", color = Danger)
                    }
                    2 -> Caption(
                        "0–70 ms: silence.\nThen a slow rise until ~200 ms.\nEvery scratch began with ~0.2 s of nothing.",
                        color = DeckColors.warm,
                    )
                    3 -> Caption(
                        "Drop the start and the fade-out.\nCut the loud middle into 16 grains of 120 ms,\nloud from the first few ms.",
                        color = DeckColors.textPrimary,
                    )
                    else -> MorphingCodeBlock(code = GRAIN_CODE, fontSize = 22.sp, modifier = Modifier.fillMaxWidth())
                }
            }
            Column(Modifier.weight(1f)) {
                if (step >= 4) {
                    val scratch = remember { ScratchState(ScratchConfig.Coin) }
                    LabeledDemo("scratch it — sound on", good = true) {
                        ScratchPanel(
                            scratch,
                            { tilt.value },
                            dust = DustGate.Swept,
                            sound = true,
                            modifier = Modifier.fillMaxWidth().height(360.dp),
                        )
                    }
                    Gap(24.dp)
                    Caption("Fast swipe → a burst. Slow drag → a few scrapes.\nBare paper → silence.", color = Good)
                    Gap(16.dp)
                    ResetButton(scratch::reset)
                }
            }
        }
    }
}

/**
 * The recording as a waveform: peak per bucket, mirrored. [window] is the time range shown, in
 * seconds. Lead-in marks the silence and the slow rise; grains mark the 16 cuts.
 */
@Composable
private fun Waveform(
    peaks: FloatArray,
    seconds: Float,
    window: Pair<Float, Float>,
    showLeadIn: Boolean,
    showGrains: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.background(PreviewBackground, RoundedCornerShape(16.dp))) {
        if (peaks.isEmpty() || seconds <= 0f) return@Canvas
        val (from, to) = window
        fun x(t: Float) = (t - from) / (to - from) * size.width
        val mid = size.height / 2f

        if (showLeadIn) {
            drawRect(Danger.copy(alpha = 0.25f), topLeft = Offset(x(0f), 0f), size = Size(x(0.07f) - x(0f), size.height))
            drawRect(DeckColors.warm.copy(alpha = 0.18f), topLeft = Offset(x(0.07f), 0f), size = Size(x(0.2f) - x(0.07f), size.height))
        }
        if (showGrains) {
            drawRect(Color.Black.copy(alpha = 0.35f), topLeft = Offset(0f, 0f), size = Size(x(TRIM_START), size.height))
            val end = TRIM_START + GRAIN * GRAINS
            drawRect(Color.Black.copy(alpha = 0.35f), topLeft = Offset(x(end), 0f), size = Size(size.width - x(end), size.height))
        }

        val first = (from / seconds * peaks.size).toInt().coerceIn(0, peaks.lastIndex)
        val last = (to / seconds * peaks.size).toInt().coerceIn(first + 1, peaks.size)
        val count = last - first
        val w = size.width / count
        for (i in 0 until count) {
            val h = peaks[first + i] * mid * 0.95f
            drawRect(DeckColors.accentAlt, topLeft = Offset(i * w, mid - h), size = Size(maxOf(w - 1f, 1f), maxOf(h * 2f, 1f)))
        }

        if (showGrains) {
            for (g in 0..GRAINS) {
                val gx = x(TRIM_START + g * GRAIN)
                drawLine(Color.White.copy(alpha = 0.7f), Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 2.dp.toPx())
            }
        }
    }
}

val soundSlides: List<Slide> = listOf(
    chapterSlide("It sounds like foil", "the last ingredient"),
    soundSlide,
)
