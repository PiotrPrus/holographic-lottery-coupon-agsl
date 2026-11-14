package dev.piotrprus.holoscratch.presentation.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Alpha of lines outside the current highlight — Keynote's "dimmed code" look. */
private const val DIMMED_ALPHA = 0.25f

/**
 * [CodeBlock] variant for stepping through a snippet: lines in [highlight]
 * (0-based line indices) stay fully opaque, the rest fade to a dimmed state.
 * Pass `null` to show every line at full strength. [dimAll] fades the whole
 * block, e.g. while a preview video plays on top of it.
 */
@Composable
fun StepCodeBlock(
    code: String,
    modifier: Modifier = Modifier,
    highlight: Set<Int>? = null,
    dimAll: Boolean = false,
    fontSize: TextUnit = 26.sp,
) {
    val lines = remember(code) { highlightKotlin(code.trimIndent()).splitLines() }
    Box(
        modifier = modifier
            .background(DeckColors.codeBackground, RoundedCornerShape(20.dp))
            .border(1.dp, DeckColors.codeBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 40.dp, vertical = 32.dp),
    ) {
        Column {
            lines.forEachIndexed { index, line ->
                val emphasized = !dimAll && (highlight == null || index in highlight)
                val alpha by animateFloatAsState(
                    targetValue = if (emphasized) 1f else DIMMED_ALPHA,
                    animationSpec = tween(durationMillis = 400),
                )
                Text(
                    text = line,
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize,
                    lineHeight = fontSize * 1.45f,
                    softWrap = false,
                    modifier = Modifier.graphicsLayer { this.alpha = alpha },
                )
            }
        }
    }
}

/** Convenience for building a highlight set from line ranges: `lines(0..2, 5)`. */
fun lines(vararg parts: Any): Set<Int> = buildSet {
    parts.forEach { part ->
        when (part) {
            is Int -> add(part)
            is IntRange -> addAll(part)
            else -> error("Unsupported highlight part: $part")
        }
    }
}

private fun AnnotatedString.splitLines(): List<AnnotatedString> {
    val result = mutableListOf<AnnotatedString>()
    var start = 0
    while (true) {
        val end = text.indexOf('\n', start)
        if (end < 0) {
            result += subSequence(start, text.length)
            return result
        }
        result += subSequence(start, end)
        start = end + 1
    }
}
