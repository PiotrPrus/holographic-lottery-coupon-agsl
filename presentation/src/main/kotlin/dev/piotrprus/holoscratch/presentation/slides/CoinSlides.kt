package dev.piotrprus.holoscratch.presentation.slides

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.piotrprus.holoscratch.presentation.deck.Slide
import dev.piotrprus.holoscratch.presentation.demos.FoilLook
import dev.piotrprus.holoscratch.presentation.demos.Jitter
import dev.piotrprus.holoscratch.presentation.demos.MouseTilt
import dev.piotrprus.holoscratch.presentation.demos.ScratchConfig
import dev.piotrprus.holoscratch.presentation.demos.ScratchPanel
import dev.piotrprus.holoscratch.presentation.demos.ScratchState
import dev.piotrprus.holoscratch.presentation.demos.Tool
import dev.piotrprus.holoscratch.presentation.ui.DeckColors
import dev.piotrprus.holoscratch.presentation.ui.MorphingCodeBlock
import dev.piotrprus.holoscratch.presentation.ui.StepCodeBlock
import dev.piotrprus.holoscratch.presentation.ui.lines

// Chapter 5 — "It scrapes like a coin". A marker vs a coin edge, a coin that turns, a hand that
// wobbles, and one last bug. Flat grey foil: the shape of the scrape is easier to read.

/** Panels that share one finger: scratch either and all of them follow. */
@Composable
private fun SideBySide(
    tilt: MouseTilt,
    configs: List<Pair<String, ScratchConfig>>,
    goodIndex: Int,
    visibleCount: Int = configs.size,
    height: Dp = 400.dp,
) {
    val states = remember { configs.map { ScratchState(it.second) } }
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            configs.forEachIndexed { index, (label, _) ->
                LabeledDemo(label, good = index == goodIndex, modifier = Modifier.weight(1f), visible = index < visibleCount) {
                    ScratchPanel(
                        scratch = states[index],
                        tilt = { tilt.value },
                        mirrors = states - states[index],
                        look = FoilLook.Flat,
                        modifier = Modifier.fillMaxWidth().height(height),
                    )
                }
            }
        }
        Gap(20.dp)
        ResetButton({ states.forEach { it.reset() } })
    }
}

private fun coinCode(coin: Boolean) = if (!coin) {
    """
    drawPath(
        path = scratch.path,
        color = Color.Transparent,
        blendMode = BlendMode.Clear,
        style = Stroke(width = 26.dp.toPx(), cap = StrokeCap.Round),
    )
    """
} else {
    """
    // every move: add the rectangle the coin swept
    path.moveTo(from.x + side.x, from.y + side.y)
    path.lineTo(to.x + side.x, to.y + side.y)
    path.lineTo(to.x - side.x, to.y - side.y)
    path.lineTo(from.x - side.x, from.y - side.y)
    path.close()

    drawPath(
        path = scratch.path,
        color = Color.Transparent,
        blendMode = BlendMode.Clear,   // no style = Fill
    )
    """
}

/** Marker vs coin edge. */
val coinBuildSlide = Slide(steps = 1) {
    val coin = step >= 1
    val scratch = remember(coin) {
        ScratchState(ScratchConfig(tool = if (coin) Tool.TurningBlade else Tool.RoundBrush, jitter = Jitter.Off))
    }
    val (title, subtitle) = if (coin) "Path" to "draw the shape of a coin" else "Stroke" to "a round marker"
    BuildSlide(
        title = title,
        subtitle = subtitle,
        code = coinCode(coin),
        codeWidth = 1060.dp,
        codeFontSize = 25.sp,
        previewSize = 600.dp to 380.dp,
        below = {
            if (coin) Caption("A thin rectangle — a coin edge.", color = Good)
            else Caption("Round, fat, same width everywhere.\nNobody scratches with a marker.")
        },
    ) { tilt ->
        ScratchPanel(scratch, { tilt.value }, look = FoilLook.Flat, modifier = Modifier.fillMaxSize())
    }
}

private fun turnCode(turning: Boolean) = if (!turning) {
    """
    // the coin always lies flat
    val side = Offset(halfWidth, 0f)
    """
} else {
    """
    val direction = (position - last) / moved   // where the finger goes
    val side = Offset(-direction.y, direction.x) * halfWidth   // 90° to it
    """
}

/** A real coin turns with your hand. */
val turningSlide = Slide(steps = 1) {
    LiveSlide(title = "The coin turns", subtitle = "like your hand") { tilt ->
        SideBySide(
            tilt,
            listOf(
                "always flat" to ScratchConfig(tool = Tool.FlatBlade, jitter = Jitter.Off),
                "turns with the finger" to ScratchConfig(tool = Tool.TurningBlade, jitter = Jitter.Off),
            ),
            goodIndex = 1,
            visibleCount = if (step >= 1) 2 else 1,
            height = 400.dp,
        )
        Gap(40.dp)
        MorphingCodeBlock(code = turnCode(step >= 1), fontSize = 28.sp, modifier = Modifier.fillMaxWidth())
    }
}

private const val WOBBLE_CODE = """
private const val WidthMin = 0.70f     // the coin is 70%…
private const val WidthMax = 1.20f     // …to 120% wide
private const val SideDrift = 0.18f    // and drifts sideways a bit
private const val EveryPx = 9f         // pick a new target every 9 px
private const val Follow = 0.35f       // ease toward it, don't jump
"""

/** Hands wobble. */
val wobbleSlide = Slide(steps = 2) {
    LiveSlide(
        title = "Hands wobble",
        subtitle = when (step) {
            0 -> "too perfect — that's a computer"
            1 -> "random every event — a saw blade"
            else -> "random every few pixels — a real scrape"
        },
    ) { tilt ->
        SideBySide(
            tilt,
            listOf(
                "perfect" to ScratchConfig(tool = Tool.TurningBlade, jitter = Jitter.Off),
                "random every event" to ScratchConfig(tool = Tool.TurningBlade, jitter = Jitter.PerEvent),
                "random every 9 px" to ScratchConfig(tool = Tool.TurningBlade, jitter = Jitter.PerDistance),
            ),
            goodIndex = 2,
            visibleCount = step + 1,
            height = 330.dp,
        )
        Gap(40.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(50.dp), verticalAlignment = Alignment.CenterVertically) {
            StepCodeBlock(
                code = WOBBLE_CODE,
                fontSize = 24.sp,
                highlight = when (step) {
                    0 -> null
                    1 -> lines(0, 1, 2)
                    else -> lines(3, 4)
                },
                modifier = Modifier.weight(1.4f),
            )
            Caption("Five numbers.\nThat's the whole \"hand\".", modifier = Modifier.weight(1f), color = DeckColors.textPrimary)
        }
    }
}

private fun pressCode(fixed: Boolean) = if (!fixed) {
    """
    fun start(position: Offset) {
        addCoin(position, direction = Down)   // which way? no idea yet
    }
    """
} else {
    """
    fun start(position: Offset) {
        pendingPress = true                     // wait…
    }

    fun extend(position: Offset) {
        if (pendingPress) addCoin(last, direction)   // …now we know
    }
    """
}

/** One more bug: tap and drag sideways. */
val pressSlide = Slide(steps = 2) {
    LiveSlide(
        title = "One more bug",
        subtitle = if (step < 2) null else "wait for the first move",
    ) { tilt ->
        SideBySide(
            tilt,
            listOf(
                "coin placed on press" to ScratchConfig(tool = Tool.TurningBlade, jitter = Jitter.Off, deferPress = false),
                "coin placed on first move" to ScratchConfig(tool = Tool.TurningBlade, jitter = Jitter.Off, deferPress = true),
            ),
            goodIndex = 1,
            visibleCount = if (step >= 2) 2 else 1,
            height = 320.dp,
        )
        Gap(40.dp)
        Reveal(at = 1) {
            MorphingCodeBlock(code = pressCode(step >= 2), fontSize = 24.sp, modifier = Modifier.fillMaxWidth())
        }
    }
}

val coinSlides: List<Slide> = listOf(
    chapterSlide("Nobody scratches\nwith a marker", "make it scrape like a coin"),
    coinBuildSlide,
    turningSlide,
    wobbleSlide,
    pressSlide,
    tryItSlide("Coin scratch", "h2n6zb"),
)
