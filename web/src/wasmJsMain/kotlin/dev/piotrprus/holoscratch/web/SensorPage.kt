package dev.piotrprus.holoscratch.web

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun SensorPage() {
    Page("Gravity sensor check", scrollable = true) {
        MotionStatus()
        Gap(8)
        val live = Motion.source == TiltSource.Sensor && motionEvents() > 0
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Label("gravity, m/s2", color = Dim, size = 12.sp)
                Gap(4)
                Axis("x", Motion.x)
                Axis("y", Motion.y)
                Axis("z", Motion.z)
                Gap(14)
                Label("roll  = x / g", color = Dim, size = 12.sp, mono = true)
                Label(Motion.tilt.roll.fmt(2, sign = true), size = 22.sp, weight = FontWeight.Bold, mono = true)
                Gap(6)
                Label("pitch = (z/g - 0.62) * 2.2", color = Dim, size = 12.sp, mono = true)
                Label(Motion.tilt.pitch.fmt(2, sign = true), size = 22.sp, weight = FontWeight.Bold, mono = true)
                Gap(4)
                Label("clamped to -1..1, smoothed 0.18", color = Dim, size = 11.sp)
            }
            PhoneWithGravity(Modifier.width(130.dp).height(240.dp))
        }
        Gap(16)
        if (live) {
            Label(
                "Browser raw: ${motionRawX().toFloat().fmt(2, true)}  ${motionRawY().toFloat().fmt(2, true)}  ${motionRawZ().toFloat().fmt(2, true)}",
                color = Dim, size = 11.sp, mono = true, modifier = Modifier.fillMaxWidth(),
            )
            Label(
                "sign ${if (motionSign() > 0) "+1" else "-1"} (${motionSignSource()}) to match Android",
                color = Dim, size = 11.sp, mono = true, modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Label(
                "No sensor yet: x, y, z stay 0 and the arrow uses the mouse instead.",
                color = Dim, size = 11.sp, modifier = Modifier.fillMaxWidth(),
            )
        }
        Gap(8)
        Label(
            "Same axes as Android's TYPE_GRAVITY: x to the right, y up the screen, z out of the screen. Lying flat face up: z = +9.81. Upright: y = +9.81.",
            color = Dim, size = 11.sp, modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Axis(name: String, value: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Label("$name ", color = Dim, size = 16.sp, mono = true)
        Label(value.fmt(2, sign = true), size = 20.sp, weight = FontWeight.Bold, mono = true)
    }
}

/**
 * The phone, seen from the front, with an arrow that always points at the ground.
 *
 * Gravity pulls along −(x, y, z) in device axes. On the screen plane that is (−x, −y); a canvas
 * has y growing downward, so the arrow is drawn along (−x, +y). Its length is the part of
 * gravity in the screen plane: full when upright, nothing when flat. The circle shows the rest,
 * the part going through the screen (the filled dot means "into the table", i.e. face up).
 *
 * Without a sensor, the mouse tilt is turned back into a fake gravity vector so the drawing moves.
 */
@Composable
private fun PhoneWithGravity(modifier: Modifier) {
    val live = motionEvents() > 0
    val gx: Float
    val gy: Float
    val gz: Float
    if (live) {
        gx = Motion.x; gy = Motion.y; gz = Motion.z
    } else {
        val t = Motion.tilt
        gx = t.roll * G
        gz = (t.pitch / 2.2f + 0.62f).coerceIn(-1f, 1f) * G
        gy = sqrt((G * G - gx * gx - gz * gz).coerceAtLeast(0f))
    }
    Canvas(modifier) { drawPhone(gx, gy, gz) }
}

private fun DrawScope.drawPhone(gx: Float, gy: Float, gz: Float) {
    val w = size.width * 0.78f
    val h = min(size.height * 0.96f, w * 2.05f)
    val left = (size.width - w) / 2f
    val top = (size.height - h) / 2f
    val r = CornerRadius(w * 0.16f)
    val body = Color.White.copy(alpha = 0.85f)
    drawRoundRect(Color(0xFF221E3A), Offset(left, top), Size(w, h), r)
    drawRoundRect(body, Offset(left, top), Size(w, h), r, style = Stroke(3.dp.toPx()))
    // Camera notch, so "up" is obvious.
    drawRoundRect(body, Offset(left + w * 0.38f, top + h * 0.03f), Size(w * 0.24f, h * 0.022f), CornerRadius(h))

    val c = Offset(size.width / 2f, size.height / 2f)
    val maxLen = h * 0.38f

    // Out-of-screen part: a circle whose size is |z| / g.
    val zr = (kotlin.math.abs(gz) / G).coerceIn(0f, 1f) * w * 0.22f
    if (zr > 1f) {
        if (gz > 0f) drawCircle(Accent.copy(alpha = 0.6f), zr, c) // face up: gravity goes into the screen
        else drawCircle(Accent.copy(alpha = 0.6f), zr, c, style = Stroke(2.dp.toPx()))
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
    val stroke = 5.dp.toPx()
    drawLine(Accent, c, tip, stroke, cap = StrokeCap.Round)
    val head = 14.dp.toPx()
    val px = -uy
    val py = ux
    val arrow = Path().apply {
        moveTo(tip.x + ux * head * 0.6f, tip.y + uy * head * 0.6f)
        lineTo(tip.x - ux * head * 0.5f + px * head * 0.6f, tip.y - uy * head * 0.5f + py * head * 0.6f)
        lineTo(tip.x - ux * head * 0.5f - px * head * 0.6f, tip.y - uy * head * 0.5f - py * head * 0.6f)
        close()
    }
    drawPath(arrow, Accent)
    drawCircle(Color.White, 3.dp.toPx(), c)
}
