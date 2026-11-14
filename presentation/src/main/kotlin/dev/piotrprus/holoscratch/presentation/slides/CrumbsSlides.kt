package dev.piotrprus.holoscratch.presentation.slides

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import dev.piotrprus.holoscratch.presentation.demos.FlatFoil
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.piotrprus.holoscratch.presentation.deck.Slide
import dev.piotrprus.holoscratch.presentation.demos.DustGate
import dev.piotrprus.holoscratch.presentation.demos.DustRecipe
import dev.piotrprus.holoscratch.presentation.demos.ScratchPanel
import dev.piotrprus.holoscratch.presentation.demos.ScratchState
import dev.piotrprus.holoscratch.presentation.ui.DeckColors
import dev.piotrprus.holoscratch.presentation.ui.MorphingCodeBlock
import dev.piotrprus.holoscratch.presentation.ui.StepCodeBlock
import dev.piotrprus.holoscratch.presentation.ui.lines

// Chapter 4 — "It leaves crumbs". ParticleEmitter, config grown one idea at a time (starting with
// the confetti party), then the puzzle: dust should only come off foil that is still there.

private fun emitterCode(recipe: DustRecipe) = buildList {
    add("CanvasParticleEmitter(")
    add("    config = CanvasEmitterConfig(")
    add("        particlePerSecond = 300,")
    add("        emitterCenter = fingerPosition,")
    if (recipe == DustRecipe.Defaults) {
        add("        colors = listOf(Red, Yellow, Blue, Green, Pink),")
        add("        particleSizes = listOf(10.dp, 14.dp),")
    } else {
        add("        colors = listOf(Silver, Graphite, Steel),")
        add("        particleSizes = listOf(2.dp, 3.dp, 5.dp),")
    }
    if (recipe >= DustRecipe.Spray) {
        add("        spread = -180..180,")
        add("        initialForce = 30..190,")
        add("        lifespanRange = 380..900,")
    }
    if (recipe >= DustRecipe.Full) {
        add("        gravityStrength = 1300f,")
    }
    add("    ),")
    add(")")
}.joinToString("\n")

/** ParticleEmitter config, one idea per click. Scratch the preview at every step. */
val emitterBuildSlide = Slide(steps = 3) {
    val recipe = DustRecipe.entries[step]
    val (title, subtitle) = when (recipe) {
        DustRecipe.Defaults -> "On the card" to "particles that follow the finger"
        DustRecipe.Metal -> "colors" to "crumbs of metal, not a party"
        DustRecipe.Spray -> "spread" to "flick them everywhere"
        DustRecipe.Full -> "gravity" to "metal falls"
    }
    val scratch = remember { ScratchState() }
    BuildSlide(
        title = title,
        subtitle = subtitle,
        code = emitterCode(recipe),
        codeWidth = 960.dp,
        codeFontSize = 28.sp,
        previewSize = 700.dp to 420.dp,
        below = {
            when (recipe) {
                DustRecipe.Defaults -> Caption("Straight from the README: 🎉 a confetti party.", color = DeckColors.warm)
                DustRecipe.Full -> Caption("github.com/PiotrPrus/ParticleEmitter")
                else -> Unit
            }
            Gap(24.dp)
            ResetButton(scratch::reset)
        },
    ) { tilt ->
        ScratchPanel(scratch, { tilt.value }, dust = DustGate.Always, dustRecipe = recipe, prizeScale = 0.8f, modifier = Modifier.fillMaxSize())
    }
}

/** The bug: drag over an already-scratched spot and dust still pours out. */
val dustFromNowhereSlide = Slide(steps = 1) {
    LiveSlide(title = "Spot the bug") { tilt ->
        val scratch = remember { ScratchState() }
        Row(horizontalArrangement = Arrangement.spacedBy(60.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1.4f)) {
                ScratchPanel(scratch, { tilt.value }, dust = DustGate.Always, modifier = Modifier.fillMaxWidth().height(520.dp))
                Gap(20.dp)
                ResetButton(scratch::reset)
            }
            Column(Modifier.weight(1f)) {
                Reveal(at = 1) {
                    Column {
                        Text("Dust from bare paper.", color = Danger, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                        Gap(20.dp)
                        Caption("The finger makes dust.\nThe metal should.", color = DeckColors.textPrimary)
                    }
                }
            }
        }
    }
}

/** "Is there still foil here?" — two obvious answers, both rejected, then the trick. */
val obviousAnswersSlide = Slide(steps = 3) {
    LiveSlide(title = "Is there still foil here?") {
        Column(verticalArrangement = Arrangement.spacedBy(40.dp)) {
            Reveal(at = 1) { Answer("Ask the Path", "Paths can't answer \"am I covering this point?\" cheaply.", good = false) }
            Reveal(at = 2) { Answer("Read the pixel", "Pixels live on the GPU. Reading them back is slow.", good = false) }
            Reveal(at = 3) { Answer("Keep notes", "Write down every spot we've scratched. Like graph paper.", good = true) }
        }
    }
}

@Composable
private fun Answer(title: String, why: String, good: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(if (good) "✓" else "✗", color = if (good) Good else Danger, fontSize = 72.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(100.dp))
        Column {
            Text(title, color = DeckColors.textPrimary, fontSize = 52.sp, fontWeight = FontWeight.SemiBold)
            Text(why, color = DeckColors.textSecondary, fontSize = 32.sp)
        }
    }
}

private const val GRID_CODE = """
private const val CellPx = 10f
private val cleared = BooleanArray(cols * rows)

fun clearAlong(from: Offset, to: Offset, radius: Float): Int {
    var newlyCleared = 0
    for (cell in cellsNear(from, to, radius)) {
        if (!cleared[cell]) {
            cleared[cell] = true
            newlyCleared++
        }
    }
    return newlyCleared
}
"""

/** The graph paper, visible. */
val gridSlide = Slide(steps = 1) {
    LiveSlide(title = "BooleanArray", subtitle = "graph paper under the foil") { tilt ->
        val scratch = remember { ScratchState() }
        Row(horizontalArrangement = Arrangement.spacedBy(50.dp)) {
            Column(Modifier.weight(1f)) {
                StepCodeBlock(
                    code = GRID_CODE,
                    fontSize = 24.sp,
                    highlight = if (step >= 1) lines(4, 8, 11) else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Gap(30.dp)
                Reveal(at = 1) {
                    Caption("It even counts how many squares were still covered. Hold that thought.", color = DeckColors.warm)
                }
            }
            LabeledDemo("cyan = noted", modifier = Modifier.weight(1f)) {
                ScratchPanel(scratch, { tilt.value }, showGrid = true, modifier = Modifier.fillMaxWidth().height(500.dp))
            }
        }
    }
}

private fun freshCode(stage: Int) = buildList {
    when (stage) {
        0 -> add("// is there still foil under the brush?")
        1 -> {
            add("// idea #1: how much of the brush is on foil?")
            add("val covered = foilUnderBrush / cellsUnderBrush")
        }
        else -> {
            add("// idea #2: how much NEW foil did this move clear?")
            add("val newlyCleared = clearAlong(last, position, radius)")
            add("val couldClear = brushWidth * moved / (CellPx * CellPx)")
            add("val fresh = newlyCleared / couldClear")
            if (stage >= 3) {
                add("")
                add("particlePerSecond = (300 * fresh).roundToInt()")
            }
        }
    }
}.joinToString("\n")

/**
 * Which number should drive the dust? 0 the question, and a picture of one brush move on the
 * graph paper · 1 idea #1, count foil under the brush: near 0 even on new foil · 2 idea #2, count
 * only the cells this move cleared: 1 on new foil, 0 on bare paper · 3 that number drives the dust.
 */
val freshSlide = Slide(steps = 3) {
    val (title, subtitle) = when (step) {
        0 -> "Fresh foil?" to "one brush move, on the graph paper"
        1 -> "Idea #1" to "how much of the brush is on foil?"
        2 -> "Idea #2" to "how much NEW foil did this move clear?"
        else -> "particlePerSecond" to "dust = how much fresh foil"
    }
    LiveSlide(title = title, subtitle = subtitle) { tilt ->
        val scratch = remember { ScratchState() }
        Row(horizontalArrangement = Arrangement.spacedBy(50.dp)) {
            Column(Modifier.weight(1f)) {
                MorphingCodeBlock(code = freshCode(step), fontSize = 22.sp, modifier = Modifier.fillMaxWidth())
                Gap(24.dp)
                BrushMoveDiagram(
                    mode = when (step) {
                        0 -> BrushMode.Plain
                        1 -> BrushMode.UnderBrush
                        else -> BrushMode.NewOnly
                    },
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                )
                Gap(20.dp)
                when (step) {
                    0 -> Caption("Cyan = already scratched. The brush moved a few px.\nWhich number tells us it is scraping metal?", color = DeckColors.textPrimary)
                    1 -> Caption("Count foil cells inside the brush.\nIt sits on its own trail → close to 0, even on new foil.", color = Danger)
                    2 -> Caption("Count only the cells this move cleared (green),\nout of what it could clear on new foil. New foil → 1. Bare paper → 0.", color = DeckColors.textPrimary)
                    else -> Caption("Fresh foil = full dust. Bare paper = none.", color = Good)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(26.dp)) {
                Reveal(at = 1) {
                    LabeledDemo("the graph paper, live") {
                        ScratchPanel(
                            scratch,
                            { tilt.value },
                            showGrid = true,
                            dust = if (step >= 3) DustGate.Swept else DustGate.Off,
                            modifier = Modifier.fillMaxWidth().height(330.dp),
                        )
                    }
                }
                if (step >= 1) Meter("idea #1", scratch.lastNaive, Danger)
                if (step >= 2) Meter("idea #2", scratch.lastFreshness, Good)
                if (step >= 1) ResetButton(scratch::reset)
            }
        }
    }
}

private enum class BrushMode { Plain, UnderBrush, NewOnly }

/**
 * One brush move on the graph paper: the trail so far (cyan), the brush now (white circle) and,
 * depending on [mode], the cells idea #1 counts (all under the brush) or idea #2 counts (only the
 * new ones, green).
 */
@Composable
private fun BrushMoveDiagram(mode: BrushMode, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier.background(PreviewBackground, RoundedCornerShape(16.dp))) {
        val cols = 20
        val cell = size.width / cols
        val rows = (size.height / cell).toInt()
        val midY = rows / 2f * cell
        val r = 2.6f * cell
        val now = Offset(12.5f * cell, midY)
        val before = Offset(12.0f * cell, midY)
        val trailStart = Offset(3f * cell, midY)
        fun distToTrail(p: Offset): Float {
            val t = ((p.x - trailStart.x) / (before.x - trailStart.x)).coerceIn(0f, 1f)
            return (p - Offset(trailStart.x + t * (before.x - trailStart.x), midY)).getDistance()
        }
        var underCount = 0
        var foilUnder = 0
        var newCount = 0
        for (rIdx in 0 until rows) for (c in 0 until cols) {
            val center = Offset((c + 0.5f) * cell, (rIdx + 0.5f) * cell)
            val cleared = distToTrail(center) <= r
            val under = (center - now).getDistance() <= r
            if (under) underCount++
            if (under && !cleared) {
                foilUnder++
                newCount++
            }
            val color = when {
                mode == BrushMode.NewOnly && under && !cleared -> Good
                cleared -> Color(0x8800B8D4)
                else -> FlatFoil
            }
            drawRect(color, topLeft = Offset(c * cell + 1f, rIdx * cell + 1f), size = Size(cell - 2f, cell - 2f))
            if (mode == BrushMode.UnderBrush && under) {
                drawRect(Color.White, topLeft = Offset(c * cell + 1f, rIdx * cell + 1f), size = Size(cell - 2f, cell - 2f), style = Stroke(2.dp.toPx()))
            }
        }
        drawCircle(Color.White, radius = r, center = now, style = Stroke(3.dp.toPx()))
        drawLine(Color.White, before, now, strokeWidth = 3.dp.toPx())
        // What each idea would report for this one move.
        val couldClear = 2f * r * (now.x - before.x) / (cell * cell)
        val label = when (mode) {
            BrushMode.Plain -> null
            BrushMode.UnderBrush -> "foil under brush: $foilUnder / $underCount = %.2f".format(foilUnder / underCount.toFloat())
            BrushMode.NewOnly -> "new cells: $newCount / ~%.0f possible ≈ %.1f".format(couldClear, (newCount / couldClear).coerceAtMost(1f))
        }
        if (label != null) {
            drawText(
                measurer,
                label,
                topLeft = Offset(12.dp.toPx(), size.height - 34.dp.toPx()),
                style = TextStyle(color = Color.White, fontSize = 20.sp, fontFamily = FontFamily.Monospace, background = Color.Black.copy(alpha = 0.6f)),
            )
        }
    }
}

/** Before and after, one finger. */
val dustFixedSlide = Slide {
    LiveSlide(title = "Dust from the metal", subtitle = "before and after") { tilt ->
        val always = remember { ScratchState() }
        val fresh = remember { ScratchState() }
        Row(horizontalArrangement = Arrangement.spacedBy(50.dp)) {
            LabeledDemo("before", good = false, modifier = Modifier.weight(1f)) {
                ScratchPanel(always, { tilt.value }, mirrors = listOf(fresh), dust = DustGate.Always, modifier = Modifier.fillMaxWidth().height(440.dp))
            }
            LabeledDemo("after", good = true, modifier = Modifier.weight(1f)) {
                ScratchPanel(fresh, { tilt.value }, mirrors = listOf(always), dust = DustGate.Swept, modifier = Modifier.fillMaxWidth().height(440.dp))
            }
        }
    }
}

val crumbsSlides: List<Slide> = listOf(
    chapterSlide("Every scratch leaves crumbs", "particles that know where the metal is"),
    particleShowcaseSlide,
    emitterParamsSlide,
    emitterBuildSlide,
    dustFromNowhereSlide,
    obviousAnswersSlide,
    gridSlide,
    freshSlide,
    dustFixedSlide,
)
