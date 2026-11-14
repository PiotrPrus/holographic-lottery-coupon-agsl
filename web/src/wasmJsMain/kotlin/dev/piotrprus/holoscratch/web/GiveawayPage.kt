package dev.piotrprus.holoscratch.web

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

/** One card in [WinOdds] wins a goodie bag: 1 in 7 ≈ 14%. */
private const val WinOdds = 7

private val BagGold = Color(0xFFE9A93B)
private val BagGoldDark = Color(0xFFB97A1C)

/**
 * The giveaway: the full card (coin scratch, holo foil, dust, sound, vibration) with a real draw
 * under the foil. Each card is drawn once, when it appears, so scratching can't change it. Reset
 * deals a fresh card with a fresh draw.
 */
@Composable
fun GiveawayPage() {
    var card by remember { mutableIntStateOf(0) }
    key(card) {
        // `#/w4b8gx?win` / `?lose` force the result, but only on localhost, to check both cards.
        val won = remember {
            val hash = locationHash()
            when {
                isLocalhost() && hash.endsWith("?win") -> true
                isLocalhost() && hash.endsWith("?lose") -> false
                else -> Random.nextInt(WinOdds) == 0
            }
        }
        val scratch = remember { ScratchState() }
        ScratchSound(scratch)
        Page("Scratch to win") {
            Label("One card in $WinOdds wins a goodie bag.", color = Dim, size = 14.sp)
            Gap(16)
            Coupon(
                Modifier.perspectiveTilt(),
                subtitle = "A goodie bag could be under the foil.",
                prize = { if (won) BagPrize() else TryAgain() },
                overlay = { ScratchDust(scratch = scratch, modifier = Modifier.matchParentSize()) },
            ) {
                HoloFoilPanel(tilt = { Motion.tilt }, scratch = scratch, modifier = Modifier.fillMaxSize()) { ScratchHereHint() }
            }
            Gap(18)
            PillButton("New card", onClick = { card++ })
            // `?stats` on localhost: 10 000 test draws, to check the odds really are 1 in [WinOdds].
            if (isLocalhost() && locationHash().endsWith("?stats")) {
                val wins = remember { (1..10_000).count { Random.nextInt(WinOdds) == 0 } }
                Gap(8)
                Label("test draws: $wins wins / 10000 · this card: ${if (won) "WIN" else "lose"}", color = Dim, size = 12.sp)
            }
            Gap(10)
            MotionStatus()
        }
    }
}

@Composable
private fun BoxScope.BagPrize() {
    Row(
        Modifier.align(Alignment.Center),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        BagIcon(Modifier.size(96.dp))
        Column {
            Label("YOU WON", color = Ink, size = 34.sp, weight = FontWeight.Black)
            Label("a goodie bag", color = Muted, size = 14.sp)
        }
    }
}

@Composable
private fun BoxScope.TryAgain() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Label("TRY AGAIN", color = Ink.copy(alpha = 0.75f), size = 34.sp, weight = FontWeight.Black, align = TextAlign.Center)
    }
}

/** A gift bag: tapered body, a fold at the top, and a rope handle. */
@Composable
private fun BagIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val top = h * 0.32f
        // Handle.
        drawArc(
            color = BagGoldDark,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.3f, h * 0.08f),
            size = Size(w * 0.4f, h * 0.48f),
            style = Stroke(width = w * 0.06f, cap = StrokeCap.Round),
        )
        // Body: a little wider at the bottom.
        val body = Path().apply {
            moveTo(w * 0.18f, top)
            lineTo(w * 0.82f, top)
            lineTo(w * 0.9f, h * 0.94f)
            lineTo(w * 0.1f, h * 0.94f)
            close()
        }
        drawPath(body, BagGold)
        // Fold at the top edge.
        drawRoundRect(BagGoldDark, Offset(w * 0.18f, top), Size(w * 0.64f, h * 0.08f), CornerRadius(w * 0.02f))
        // A bow.
        val c = Offset(w * 0.5f, h * 0.62f)
        drawCircle(Accent, w * 0.07f, c)
        drawCircle(Accent, w * 0.06f, c + Offset(-w * 0.1f, -h * 0.03f))
        drawCircle(Accent, w * 0.06f, c + Offset(w * 0.1f, -h * 0.03f))
    }
}
