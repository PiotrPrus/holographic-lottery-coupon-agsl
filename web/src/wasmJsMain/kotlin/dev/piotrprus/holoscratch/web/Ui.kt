package dev.piotrprus.holoscratch.web

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF1B1A2E)
val Paper = Color(0xFFF7F1E4)
val Accent = Color(0xFFE0493B)
val Muted = Color(0xFF7A7690)
val PrintArea = Color(0xFFEFE7D6)
val Dim = Color.White.copy(alpha = 0.6f)

val Background = Brush.verticalGradient(listOf(Color(0xFF15142A), Color(0xFF2A1F45)))

@Composable
fun Label(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: TextUnit = 15.sp,
    weight: FontWeight = FontWeight.Normal,
    mono: Boolean = false,
    align: TextAlign = TextAlign.Start,
    letterSpacing: TextUnit = TextUnit.Unspecified,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = size,
            fontWeight = weight,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            textAlign = align,
            letterSpacing = letterSpacing,
            lineHeight = size * 1.35f,
        ),
    )
}

@Composable
fun PillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = false) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .then(if (filled) Modifier.background(Accent) else Modifier.border(1.dp, Dim, RoundedCornerShape(50)))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Label(text, color = Color.White, size = 14.sp, weight = FontWeight.Bold)
    }
}

/**
 * Common page frame: a back link to the index, a title, and a centred column.
 * [scrollable] is off on scratch pages so a drag never scrolls the page instead of scratching.
 */
@Composable
fun Page(
    title: String,
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    // No link to other demos and no "n of 5": each QR code opens one demo, and the audience
    // should not peek at the next ones before the talk gets there.
    Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Label(title, size = 22.sp, weight = FontWeight.Black, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

/**
 * Explains where the tilt is coming from, and offers the iOS permission tap.
 * The button itself does nothing in Kotlin: holo.js asks for permission on *any* tap, because
 * iOS only allows it inside the browser's own gesture handler.
 */
@Composable
fun MotionStatus(modifier: Modifier = Modifier) {
    when (Motion.source) {
        TiltSource.Sensor -> Unit
        TiltSource.NeedsPermission -> Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            PillButton("Enable motion", onClick = {}, filled = true)
            Spacer(Modifier.height(6.dp))
            Label("iPhone asks before a page may read the motion sensor.", color = Dim, size = 12.sp)
        }
        TiltSource.Denied -> Label(
            "Motion access was denied. Reload the page and tap Allow.",
            color = Dim, size = 12.sp, modifier = modifier, align = TextAlign.Center,
        )
        TiltSource.Pointer -> Label(
            "No motion sensor here: move the mouse to tilt.",
            color = Dim, size = 12.sp, modifier = modifier, align = TextAlign.Center,
        )
    }
}

@Composable
fun TiltReadout(modifier: Modifier = Modifier) {
    val t = Motion.tilt
    Label("roll ${t.roll.fmt(2, sign = true)}   pitch ${t.pitch.fmt(2, sign = true)}", color = Dim, size = 13.sp, mono = true, modifier = modifier)
}

/** The prize printed on the ticket, under the foil. */
@Composable
fun BoxScope.Prize() {
    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
        Label("YOU WON", color = Color.Black, size = 14.sp, weight = FontWeight.Bold, letterSpacing = 3.sp)
        Label("$100", color = Color.Black, size = 56.sp, weight = FontWeight.Black)
    }
}

/** Number formatting without String.format (not available on Wasm). */
fun Float.fmt(decimals: Int, sign: Boolean = false): String {
    var p = 1
    repeat(decimals) { p *= 10 }
    val scaled = kotlin.math.round(kotlin.math.abs(this) * p).toInt()
    val whole = scaled / p
    val frac = (scaled % p).toString().padStart(decimals, '0')
    val neg = this < 0f && scaled != 0
    val s = if (sign) (if (neg) "-" else "+") else (if (neg) "-" else "")
    return if (decimals == 0) "$s$whole" else "$s$whole.$frac"
}

@Composable
fun Gap(h: Int) = Spacer(Modifier.height(h.dp))
