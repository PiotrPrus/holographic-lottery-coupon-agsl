package dev.piotrprus.holoscratch.presentation.slides

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import dev.piotrprus.holoscratch.presentation.demos.Tilt
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import dev.piotrprus.holoscratch.presentation.demos.FlatFoil
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.piotrprus.holoscratch.presentation.deck.Slide
import dev.piotrprus.holoscratch.presentation.demos.Erase
import dev.piotrprus.holoscratch.presentation.demos.FoilLook
import dev.piotrprus.holoscratch.presentation.demos.ScratchConfig
import dev.piotrprus.holoscratch.presentation.demos.ScratchPanel
import dev.piotrprus.holoscratch.presentation.demos.ScratchState
import dev.piotrprus.holoscratch.presentation.demos.Tool
import dev.piotrprus.holoscratch.presentation.ui.DeckColors
import dev.piotrprus.holoscratch.presentation.ui.MorphingCodeBlock

// Chapter 1 — "It scratches". One composable, built a modifier at a time, BYCCTL style: the
// snippet grows on the left, the preview runs exactly that code. Scratch it live at every step.

/**
 * The scratch card, as the code grows: the prize, a grey foil, the finger, the foil's own
 * offscreen buffer, and finally BlendMode.Clear inside that buffer.
 */
private fun scratchCardCode(foil: Boolean, input: Boolean, offscreen: Boolean, clear: Boolean) = buildList {
    add("Box {")
    add("    Prize()   // \"YOU WON \$100\"")
    if (foil) add("    Foil(scratch)")
    add("}")
    if (foil) {
        add("")
        add("@Composable")
        add("fun Foil(scratch: ScratchState) = Box(")
        add("    Modifier")
        add("        .fillMaxSize()")
        if (input) {
            add("        .pointerInput(Unit) {")
            add("            detectDragGestures(")
            add("                onDragStart = { scratch.start(it) },")
            add("                onDrag = { drag, _ -> scratch.extend(drag.position) },")
            add("            )")
            add("        }")
        }
        if (offscreen) {
            add("        .graphicsLayer {")
            add("            compositingStrategy = CompositingStrategy.Offscreen")
            add("        }")
        }
        add("        .drawWithCache {")
        add("            onDrawWithContent {")
        add("                drawRect(Silver)")
        if (input) {
            add("                drawPath(")
            add("                    path = scratch.path,")
            add("                    color = Graphite,")
            add("                    style = brushStroke,")
            add(if (clear) "                    blendMode = BlendMode.Clear," else "                    blendMode = BlendMode.SrcOver, // default")
            add("                )")
        }
        add("            }")
        add("        }")
        add(")")
    }
}.joinToString("\n")

private const val OFFSCREEN_KDOC =
    "Rendering of content will always be rendered into an offscreen buffer first, then drawn to " +
        "the destination regardless of the other parameters configured on the graphics layer. " +
        "This is useful for leveraging different blending algorithms for masking content."

private const val CLEAR_KDOC = "Drop both the source and destination images, leaving nothing."

/**
 * From buffer to blend mode. Steps: 0 prize · 1 grey foil · 2 pointerInput (paints a line) ·
 * 3 CompositingStrategy.Offscreen (looks the same, for now) · 4 Offscreen KDoc · 5 what a blend
 * mode is: source + destination, the code and the diagram both on SrcOver · 6 one line becomes
 * Clear, the diagram adds Clear next to SrcOver, Clear's KDoc · 7 BlendMode.Clear live: a real hole.
 */
val scratchBuildSlide = Slide(steps = 7) {
    val offscreen = step >= 3
    val explainBlend = step == 5 || step == 6
    val codeClear = step >= 6
    val clear = step >= 7
    val scratch = remember { ScratchState(ScratchConfig(tool = Tool.RoundBrush)) }
    val (title, subtitle) = when (step) {
        0 -> "Box" to "the prize, printed on the ticket"
        1 -> "drawWithCache" to "a grey layer on top"
        2 -> "pointerInput" to "follow the finger"
        3, 4 -> "CompositingStrategy" to "give the foil its own buffer"
        5 -> "BlendMode" to "source + destination = result"
        else -> "BlendMode.Clear" to "erase, don't paint"
    }
    val code = scratchCardCode(foil = step >= 1, input = step >= 2, offscreen = offscreen, clear = codeClear)
    // The blend steps light up the one line that changes.
    val blendLine = code.lines().indexOfFirst { "blendMode =" in it }
    BuildSlide(
        title = title,
        subtitle = subtitle,
        code = code,
        highlight = if ((explainBlend || codeClear) && blendLine >= 0) setOf(blendLine) else null,
        codeFontSize = 23.sp,
        previewLabel = if (explainBlend) "How Clear blends" else "Preview",
        previewSize = if (explainBlend) 560.dp to 280.dp else 560.dp to 360.dp,
        overlay = { KDocOverlay(visible = step == 4, term = "CompositingStrategy.Offscreen", text = OFFSCREEN_KDOC) },
        below = {
            when (step) {
                1 -> Caption("Just grey paint on top of the prize.")
                2 -> Caption("We drew on the foil.\nWe didn't scratch it.", color = DeckColors.warm)
                3 -> Caption("The foil now draws into its own buffer.\nLooks the same… for now.")
                4 -> Caption("A separate picture of just the foil,\nput on screen when it's done.")
                5 -> Caption("Source: the path we draw now.\nDestination: the foil's buffer.\nSrcOver (the default) paints on top.", color = DeckColors.textPrimary)
                6 -> Column(Modifier.width(560.dp)) {
                    Caption("Clear doesn't paint. It erases.", color = DeckColors.textPrimary)
                    Gap(16.dp)
                    KDocCallout("BlendMode.Clear", CLEAR_KDOC)
                }
                7 -> Caption("There it is. 🎉\nClear erases inside the buffer only.", color = Good)
                else -> Unit
            }
        },
    ) { tilt ->
        if (explainBlend) {
            BlendModeDiagram(showClear = codeClear, modifier = Modifier.fillMaxSize())
            return@BuildSlide
        }
        ScratchPanel(
            scratch = scratch,
            tilt = { tilt.value },
            look = if (step == 0) FoilLook.None else FoilLook.Flat,
            erase = when {
                step < 2 -> Erase.None
                clear -> Erase.Clear
                else -> Erase.Paint
            },
            offscreen = offscreen,
            prizeScale = 0.8f,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun historyCode(withHistory: Boolean) = buildList {
    add(".pointerInput(Unit) {")
    add("    detectDragGestures(")
    add("        onDragStart = { scratch.start(it) },")
    add("        onDrag = { change, _ ->")
    if (withHistory) {
        add("            change.historical.forEach {")
        add("                scratch.extend(it.position)")
        add("            }")
    }
    add("            scratch.extend(change.position)")
    add("        },")
    add("    )")
    add("}")
}.joinToString("\n")

private const val HISTORICAL_KDOC =
    "Optional high-frequency pointer moves in between the last two dispatched events. " +
        "Can be used for extra accuracy when touchscreen rate exceeds framerate."

/**
 * change.historical, one beat per click. 0 the code · 1 a fast swipe as the app sees it: one point
 * per frame, straight chords · 2 the KDoc and the in-between points · 3 replay them: the code grows
 * and the line follows the curve · 4 live: the same stroke scratched without and with historical.
 */
val historicalSlide = Slide(steps = 4) {
    LiveSlide(
        title = "change.historical",
        subtitle = when (step) {
            0 -> "how the finger reaches us"
            1 -> "one event per frame"
            2 -> "the points in between"
            3 -> "replay them, oldest first"
            else -> "scratch fast circles"
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(50.dp)) {
            Column(Modifier.weight(1f)) {
                MorphingCodeBlock(code = historyCode(withHistory = step >= 3), fontSize = 24.sp, modifier = Modifier.fillMaxWidth())
                Gap(30.dp)
                Reveal(at = 2) { KDocCallout("PointerInputChange.historical", HISTORICAL_KDOC) }
            }
            if (step >= 4) {
                HistoryComparison(Modifier.weight(1f))
                return@Row
            }
            Column(Modifier.weight(1f)) {
                Reveal(at = 1) {
                    LabeledDemo("a fast swipe", good = if (step >= 3) true else null) {
                        HistoryDiagram(showHistory = step >= 2, connectHistory = step >= 3, modifier = Modifier.fillMaxWidth().height(340.dp))
                    }
                }
                Gap(30.dp)
                when (step) {
                    1 -> Caption(
                        "The screen samples your finger faster\nthan the app draws frames.\nWe only get the big dots: straight chords.",
                        color = DeckColors.textPrimary,
                    )
                    2 -> Caption(
                        "The small dots are batched into the event:\nchange.historical, oldest first.",
                        color = DeckColors.textPrimary,
                    )
                    3 -> Caption(
                        "Replay them before change.position.\nThe scratch follows the real curve.",
                        color = Good,
                    )
                    else -> Unit
                }
            }
        }
    }
}

/**
 * One stroke, two panels: the top one only gets change.position once per frame, the bottom one
 * replays every historical point too. Same pointer input drives both.
 */
/** A thin tip, so the corners of a per-frame stroke are easy to see. */
private val HistoryBrush = 8.dp

@Composable
private fun HistoryComparison(modifier: Modifier = Modifier) {
    val perFrame = remember { ScratchState(ScratchConfig(tool = Tool.RoundBrush)) }
    val everyPoint = remember { ScratchState(ScratchConfig(tool = Tool.RoundBrush)) }
    val both = listOf(perFrame, everyPoint)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        LabeledDemo("only change.position", good = false) {
            ScratchPanel(perFrame, { Tilt.Zero }, mirrors = listOf(everyPoint), framePoints = listOf(perFrame), look = FoilLook.Flat, bladeLength = HistoryBrush, prizeScale = 0.6f, modifier = Modifier.fillMaxWidth().height(250.dp))
        }
        LabeledDemo("+ change.historical", good = true) {
            ScratchPanel(everyPoint, { Tilt.Zero }, mirrors = listOf(perFrame), framePoints = listOf(perFrame), look = FoilLook.Flat, bladeLength = HistoryBrush, prizeScale = 0.6f, modifier = Modifier.fillMaxWidth().height(250.dp))
        }
        ResetButton({ both.forEach { it.reset() } })
    }
}

/**
 * A fast curved swipe. Big dots: what one-event-per-frame sees. Small dots: the historical points
 * in between. Lines show what the scratch would follow.
 */
@Composable
private fun HistoryDiagram(showHistory: Boolean, connectHistory: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.background(PreviewBackground, RoundedCornerShape(16.dp))) {
        val samples = 25
        val points = (0..samples).map { i ->
            val t = i / samples.toFloat()
            Offset(
                x = size.width * (0.08f + 0.84f * t),
                y = size.height * (0.5f + 0.32f * sin(t * 2f * PI.toFloat() * 1.1f)),
            )
        }
        val perFrame = points.filterIndexed { i, _ -> i % 5 == 0 }
        val line = Path().apply {
            val route = if (connectHistory) points else perFrame
            moveTo(route.first().x, route.first().y)
            route.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            line,
            if (connectHistory) Good else Danger,
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        if (showHistory) points.forEach { drawCircle(DeckColors.accentAlt, radius = 4.dp.toPx(), center = it) }
        perFrame.forEach { drawCircle(Color.White, radius = 9.dp.toPx(), center = it) }
    }
}

/**
 * BlendMode in one picture: destination (the foil), source (the path), and what Clear leaves.
 * The checkerboard is "transparent". Inside an offscreen layer the hole shows through to whatever
 * is underneath.
 */
@Composable
private fun BlendModeDiagram(showClear: Boolean, modifier: Modifier = Modifier) {
    val clearAlpha by animateFloatAsState(if (showClear) 1f else 0f, tween(500))
    val circle: DrawScope.(Color, BlendMode) -> Unit = { color, mode ->
        drawCircle(color, radius = size.minDimension * 0.32f, center = Offset(size.width * 0.62f, size.height * 0.6f), blendMode = mode)
    }
    Row(
        modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DiagramCell("destination") { drawRect(FlatFoil) }
        Text("+", color = DeckColors.textSecondary, fontSize = 34.sp)
        DiagramCell("source", checker = false) { circle(DeckColors.accentAlt, BlendMode.SrcOver) }
        Text("→", color = DeckColors.textSecondary, fontSize = 34.sp)
        DiagramCell("SrcOver") {
            drawRect(FlatFoil)
            circle(DeckColors.accentAlt, BlendMode.SrcOver)
        }
        Box(Modifier.graphicsLayer { alpha = clearAlpha }) {
            DiagramCell("Clear") {
                drawRect(FlatFoil)
                circle(DeckColors.accentAlt, BlendMode.Clear)
            }
        }
    }
}

@Composable
private fun DiagramCell(label: String, checker: Boolean = true, draw: DrawScope.() -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(92.dp)) {
            if (checker) Canvas(Modifier.fillMaxSize().clipToBounds()) { drawChecker() }
            Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) { draw() }
        }
        Gap(12.dp)
        Text(label, color = DeckColors.textSecondary, fontSize = 19.sp, fontFamily = FontFamily.Monospace)
    }
}

private fun DrawScope.drawChecker() {
    val cell = 12.dp.toPx()
    val cols = (size.width / cell).toInt() + 1
    val rows = (size.height / cell).toInt() + 1
    for (r in 0 until rows) for (c in 0 until cols) {
        drawRect(
            if ((r + c) % 2 == 0) Color(0xFF3A3F4B) else Color(0xFF2A2E37),
            topLeft = Offset(c * cell, r * cell),
            size = Size(cell, cell),
        )
    }
}

val scratchSlides: List<Slide> = listOf(
    chapterSlide("Let's scratch!", "a layer you can rub away"),
    scratchBuildSlide,
    historicalSlide,
    tryItSlide("Simple scratch", "k7p2qx"),
)
