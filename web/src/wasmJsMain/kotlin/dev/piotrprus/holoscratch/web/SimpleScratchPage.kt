package dev.piotrprus.holoscratch.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures

/** A thumb-thick round pen. */
private val PenWidth = 44.dp
private val Foil = Color(0xFFA9ABB2)

/** The simplest version: one Path, stroked with round caps. */
@Stable
private class PenScratch {
    val path = Path()
    var revision by mutableIntStateOf(0)
        private set

    fun dot(at: Offset) {
        // A zero-length segment with a round cap draws a dot, so a tap scratches too.
        path.moveTo(at.x, at.y)
        path.lineTo(at.x, at.y)
        revision++
    }

    fun start(at: Offset) {
        path.moveTo(at.x, at.y)
        revision++
    }

    fun extend(to: Offset) {
        path.lineTo(to.x, to.y)
        revision++
    }

    fun reset() {
        path.reset()
        revision = 0
    }
}

@Composable
fun SimpleScratchPage(onBack: () -> Unit) {
    val pen = remember { PenScratch() }
    Page(1, "Simple scratch", onBack) {
        Label(
            "Grey paint in an offscreen layer. Your finger draws a path with BlendMode.Clear, which punches through to the prize.",
            color = Dim, size = 14.sp,
        )
        Gap(20)
        Box(
            Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(PrintArea),
        ) {
            Prize()
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(pen) {
                        detectTapGestures(onPress = { pen.dot(it) })
                    }
                    .pointerInput(pen) {
                        detectDragGestures(
                            onDragStart = { pen.start(it) },
                            onDrag = { change, _ ->
                                change.historical.forEach { pen.extend(it.position) }
                                pen.extend(change.position)
                                change.consume()
                            },
                        )
                    }
                    // Offscreen: without its own layer, Clear would erase the prize underneath too.
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawRect(Foil)
                        @Suppress("UNUSED_EXPRESSION")
                        pen.revision
                        drawPath(
                            path = pen.path,
                            color = Color.Transparent,
                            style = Stroke(width = PenWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                            blendMode = BlendMode.Clear,
                        )
                    },
            )
        }
        Gap(20)
        PillButton("Reset", onClick = { pen.reset() })
        Gap(16)
        Label(
            "drawPath(path, Color.Transparent,\n  style = Stroke(44.dp, cap = Round),\n  blendMode = BlendMode.Clear)",
            color = Dim, size = 12.sp, mono = true, align = TextAlign.Start,
        )
    }
}
