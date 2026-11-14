package dev.piotrprus.holoscratch.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/** Alpha of lines outside the focus while something is focused. */
private const val MORPH_DIMMED_ALPHA = 0.3f
private const val MORPH_DURATION = 550

private val FocusBackground = Color.White.copy(alpha = 0.07f)
private val FocusBar = Color(0xFFA9BCFF)

private class MorphLine(val id: Int, val raw: String, val text: AnnotatedString)

private class MorphEntry(val line: MorphLine, val removed: Boolean)

private class MorphFrame(
    val entries: List<MorphEntry>,
    /** Live lines (no removed ones), in final order. */
    val lines: List<MorphLine>,
    /** Ids inserted by the latest change; empty on first composition. */
    val addedIds: Set<Int>,
)

/**
 * Keeps line identity across code versions: lines that survive a change keep
 * their id (so they slide into place), new lines get fresh ids (so they expand in).
 */
private class MorphHolder {
    private var nextId = 0
    private var current: List<MorphLine> = emptyList()
    val initialIds = mutableSetOf<Int>()
    private var initialized = false

    fun update(code: String): MorphFrame {
        val highlighted = highlightKotlin(code.trimIndent()).splitCodeLines()
        val raws = highlighted.map { it.text }
        val old = current
        val matches = lcsMatches(old.map { it.raw }, raws)
        val newLines = raws.indices.map { index ->
            val oldIndex = matches[index]
            if (oldIndex != null) {
                MorphLine(old[oldIndex].id, raws[index], highlighted[index])
            } else {
                MorphLine(nextId++, raws[index], highlighted[index])
            }
        }
        val keptOld = matches.values.toSet()
        val entries = mutableListOf<MorphEntry>()
        // Interleave removed old lines right where they used to be.
        var oldCursor = 0
        newLines.forEachIndexed { index, line ->
            val oldIndex = matches[index]
            if (oldIndex != null) {
                while (oldCursor < oldIndex) {
                    if (oldCursor !in keptOld) entries += MorphEntry(old[oldCursor], removed = true)
                    oldCursor++
                }
                oldCursor = oldIndex + 1
            }
            entries += MorphEntry(line, removed = false)
        }
        while (oldCursor < old.size) {
            if (oldCursor !in keptOld) entries += MorphEntry(old[oldCursor], removed = true)
            oldCursor++
        }
        val oldIds = old.map { it.id }.toSet()
        val added = if (initialized) newLines.map { it.id }.filter { it !in oldIds }.toSet() else emptySet()
        if (!initialized) {
            initialIds += newLines.map { it.id }
            initialized = true
        }
        current = newLines
        return MorphFrame(entries, newLines, added)
    }
}

/** Maps new line index → matched old line index using a longest-common-subsequence on line text. */
private fun lcsMatches(old: List<String>, new: List<String>): Map<Int, Int> {
    val n = old.size
    val m = new.size
    val table = Array(n + 1) { IntArray(m + 1) }
    for (i in n - 1 downTo 0) {
        for (j in m - 1 downTo 0) {
            table[i][j] = if (old[i] == new[j]) {
                table[i + 1][j + 1] + 1
            } else {
                maxOf(table[i + 1][j], table[i][j + 1])
            }
        }
    }
    val result = mutableMapOf<Int, Int>()
    var i = 0
    var j = 0
    while (i < n && j < m) {
        when {
            old[i] == new[j] -> {
                result[j] = i
                i++
                j++
            }
            table[i + 1][j] >= table[i][j + 1] -> i++
            else -> j++
        }
    }
    return result
}

/**
 * Code block that *evolves* instead of cross-fading: when [code] changes, lines
 * that stay slide into their new position, new lines expand in with a focus
 * highlight and removed lines collapse away. The viewport smoothly scrolls so
 * the focused lines stay in view (give the block a bounded height to enable it).
 *
 * Focus is [highlight] (0-based line indices in the current [code]) when given;
 * otherwise the lines added by the latest change. [dimAll] fades everything,
 * e.g. while a preview sits on top of the code.
 */
@Composable
fun MorphingCodeBlock(
    code: String,
    modifier: Modifier = Modifier,
    highlight: Set<Int>? = null,
    highlightAdded: Boolean = true,
    dimAll: Boolean = false,
    fontSize: TextUnit = 26.sp,
) {
    val holder = remember { MorphHolder() }
    val frame = remember(code) { holder.update(code) }
    val density = LocalDensity.current
    val lineHeight = fontSize * 1.45f
    // Whole pixels, so the row height and the scroll math agree exactly.
    val lineHeightPx = with(density) { lineHeight.toPx() }.roundToInt().toFloat()
    val lineHeightDp = with(density) { lineHeightPx.toDp() }

    val focusIds: Set<Int> = when {
        highlight != null -> highlight.mapNotNull { frame.lines.getOrNull(it)?.id }.toSet()
        highlightAdded -> frame.addedIds
        else -> emptySet()
    }
    val anyFocus = focusIds.isNotEmpty()

    var viewportPx by remember { mutableIntStateOf(0) }
    val scroll = remember { Animatable(0f) }
    val focusIndices = frame.lines.indices.filter { frame.lines[it].id in focusIds }
    val contentPx = frame.lines.size * lineHeightPx
    val scrollTarget = if (viewportPx == 0 || contentPx <= viewportPx) {
        0f
    } else if (focusIndices.isEmpty()) {
        scroll.targetValue.coerceIn(0f, contentPx - viewportPx)
    } else {
        val focusCenter = (focusIndices.first() + focusIndices.last() + 1) / 2f * lineHeightPx
        (focusCenter - viewportPx / 2f).coerceIn(0f, contentPx - viewportPx)
    }
    LaunchedEffect(scrollTarget) {
        scroll.animateTo(scrollTarget, tween(MORPH_DURATION + 150, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = modifier
            .background(DeckColors.codeBackground, RoundedCornerShape(20.dp))
            .border(1.dp, DeckColors.codeBorder, RoundedCornerShape(20.dp))
            .padding(vertical = 32.dp),
    ) {
        Box(
            modifier = Modifier
                .clipToBounds()
                .onSizeChanged { viewportPx = it.height },
        ) {
            Column(
                modifier = Modifier
                    .wrapContentHeight(align = Alignment.Top, unbounded = true)
                    .offset { IntOffset(0, -scroll.value.roundToInt()) },
            ) {
                frame.entries.forEach { entry ->
                    key(entry.line.id) {
                        val visibility = remember {
                            MutableTransitionState(entry.line.id in holder.initialIds)
                        }
                        visibility.targetState = !entry.removed
                        AnimatedVisibility(
                            visibleState = visibility,
                            enter = expandVertically(tween(MORPH_DURATION, easing = FastOutSlowInEasing)) +
                                fadeIn(tween(MORPH_DURATION, delayMillis = MORPH_DURATION / 2)),
                            exit = shrinkVertically(tween(MORPH_DURATION, easing = FastOutSlowInEasing)) +
                                fadeOut(tween(MORPH_DURATION / 2)),
                        ) {
                            val focused = entry.line.id in focusIds
                            MorphLineRow(
                                text = entry.line.text,
                                focused = focused && !dimAll,
                                alpha = when {
                                    dimAll -> MORPH_DIMMED_ALPHA
                                    !anyFocus || focused -> 1f
                                    else -> MORPH_DIMMED_ALPHA
                                },
                                fontSize = fontSize,
                                lineHeight = lineHeight,
                                height = lineHeightDp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MorphLineRow(
    text: AnnotatedString,
    focused: Boolean,
    alpha: Float,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    height: androidx.compose.ui.unit.Dp,
) {
    val animatedAlpha by animateFloatAsState(alpha, tween(MORPH_DURATION))
    val background by animateColorAsState(
        if (focused) FocusBackground else Color.Transparent,
        tween(MORPH_DURATION),
    )
    val bar by animateColorAsState(
        if (focused) FocusBar else Color.Transparent,
        tween(MORPH_DURATION),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(background),
    ) {
        Box(
            modifier = Modifier
                .height(height)
                .padding(vertical = 2.dp)
                .background(bar)
                .padding(start = 6.dp),
        )
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize,
            lineHeight = lineHeight,
            softWrap = false,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 40.dp)
                .graphicsLayer { this.alpha = animatedAlpha },
        )
    }
}

internal fun AnnotatedString.splitCodeLines(): List<AnnotatedString> {
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
