package com.example.holoscratch.ui

import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.holoscratch.sensor.rememberDeviceTilt
import com.example.holoscratch.sensor.rememberGravityReading
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

// Same colours as the web gravity page, so the two look alike.
private val Accent = Color(0xFFE0493B)
private val Dim = Color.White.copy(alpha = 0.6f)
private val Background = Brush.verticalGradient(listOf(Color(0xFF15142A), Color(0xFF2A1F45)))
private const val G = SensorManager.STANDARD_GRAVITY

/**
 * Live check of the gravity sensor, made to be read in a screen recording.
 * Same layout as the web gravity page, scaled up about 1.5× for a phone video:
 * numbers on the left, a fixed upright phone with the gravity arrow on the right.
 *
 * x, y, z are the raw sensor values. roll and pitch come from [rememberDeviceTilt],
 * so they are smoothed exactly like the values the foil uses.
 */
@Composable
fun GravityScreen(modifier: Modifier = Modifier) {
  val reading by rememberGravityReading()
  val tilt by rememberDeviceTilt()

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(Background)
        .safeDrawingPadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp)
  ) {
    Label("Gravity sensor check", size = 32.sp, weight = FontWeight.Black)
    Spacer(Modifier.height(28.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Label("gravity, m/s2", color = Dim, size = 18.sp)
        Spacer(Modifier.height(6.dp))
        Axis("x", reading.x)
        Axis("y", reading.y)
        Axis("z", reading.z)
        Spacer(Modifier.height(20.dp))
        Label("roll  = x / g", color = Dim, size = 15.sp, mono = true)
        Label(tilt.roll.signed(2), size = 34.sp, weight = FontWeight.Bold, mono = true)
        Spacer(Modifier.height(10.dp))
        Label("pitch = (z/g - 0.62) * 2.2", color = Dim, size = 15.sp, mono = true)
        Label(tilt.pitch.signed(2), size = 34.sp, weight = FontWeight.Bold, mono = true)
        Spacer(Modifier.height(6.dp))
        Label("clamped to -1..1, smoothed 0.18", color = Dim, size = 16.sp)
      }
      Canvas(Modifier.width(150.dp).height(300.dp)) {
        drawPhone(reading.x, reading.y, reading.z)
      }
    }
    Spacer(Modifier.height(28.dp))
    Label(
      "Same axes as Android's TYPE_GRAVITY: x to the right, y up the screen, z out of the screen. " +
        "Lying flat face up: z = +9.81. Upright: y = +9.81.",
      color = Dim,
      size = 17.sp,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

/** One raw axis: dim name, then the big signed number. */
@Composable
private fun Axis(name: String, value: Float) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Label("$name ", color = Dim, size = 24.sp, mono = true)
    Label(value.signed(2), size = 30.sp, weight = FontWeight.Bold, mono = true)
  }
}

/** Plain text in the web page's style: white by default, optional monospace. */
@Composable
private fun Label(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = Color.White,
  size: TextUnit = 22.sp,
  weight: FontWeight = FontWeight.Normal,
  mono: Boolean = false,
) {
  BasicText(
    text = text,
    modifier = modifier,
    style =
      TextStyle(
        color = color,
        fontSize = size,
        fontWeight = weight,
        fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
        lineHeight = size * 1.35f,
      ),
  )
}

/**
 * The phone, seen from the front and drawn upright, with an arrow that always points at the
 * ground.
 *
 * Gravity pulls along −(x, y, z) in device axes. On the screen plane that is (−x, −y); a canvas
 * has y growing downward, so the arrow is drawn along (−x, +y). Its length is the part of
 * gravity in the screen plane: full when upright, nothing when flat. The circle shows the rest,
 * the part going through the screen (filled = face up, outline = face down).
 */
private fun DrawScope.drawPhone(gx: Float, gy: Float, gz: Float) {
  val w = size.width * 0.78f
  val h = min(size.height * 0.96f, w * 2.05f)
  val left = (size.width - w) / 2f
  val top = (size.height - h) / 2f
  val r = CornerRadius(w * 0.16f)
  val body = Color.White.copy(alpha = 0.85f)
  drawRoundRect(Color(0xFF221E3A), Offset(left, top), Size(w, h), r)
  drawRoundRect(body, Offset(left, top), Size(w, h), r, style = Stroke(4.dp.toPx()))
  // Camera notch, so "up" is obvious.
  drawRoundRect(
    body,
    Offset(left + w * 0.38f, top + h * 0.03f),
    Size(w * 0.24f, h * 0.022f),
    CornerRadius(h),
  )

  val c = Offset(size.width / 2f, size.height / 2f)
  val maxLen = h * 0.38f

  // Out-of-screen part: a circle whose size is |z| / g.
  val zr = (abs(gz) / G).coerceIn(0f, 1f) * w * 0.22f
  if (zr > 1f) {
    if (gz > 0f) drawCircle(Accent.copy(alpha = 0.6f), zr, c)
    else drawCircle(Accent.copy(alpha = 0.6f), zr, c, style = Stroke(3.dp.toPx()))
  }

  // In-screen part: the arrow.
  val dx = -gx / G
  val dy = gy / G
  val len = sqrt(dx * dx + dy * dy)
  if (len < 0.04f) return
  val ux = dx / len
  val uy = dy / len
  val l = len.coerceAtMost(1f) * maxLen
  val tip = Offset(c.x + ux * l, c.y + uy * l)
  drawLine(Accent, c, tip, 7.dp.toPx(), cap = StrokeCap.Round)
  val head = 20.dp.toPx()
  val px = -uy
  val py = ux
  val arrow =
    Path().apply {
      moveTo(tip.x + ux * head * 0.6f, tip.y + uy * head * 0.6f)
      lineTo(tip.x - ux * head * 0.5f + px * head * 0.6f, tip.y - uy * head * 0.5f + py * head * 0.6f)
      lineTo(tip.x - ux * head * 0.5f - px * head * 0.6f, tip.y - uy * head * 0.5f - py * head * 0.6f)
      close()
    }
  drawPath(arrow, Accent)
  drawCircle(Color.White, 4.dp.toPx(), c)
}

/** Fixed decimals with an explicit sign, so the numbers do not jump around. */
private fun Float.signed(decimals: Int): String {
  // Avoid "-0.00" flicker around zero.
  val v = if (abs(this) < 0.5f * Math.pow(10.0, -decimals.toDouble()).toFloat()) 0f else this
  return String.format(Locale.US, "%+.${decimals}f", v)
}
