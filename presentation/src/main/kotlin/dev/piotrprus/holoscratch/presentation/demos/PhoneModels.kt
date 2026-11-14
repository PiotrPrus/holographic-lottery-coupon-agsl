package dev.piotrprus.holoscratch.presentation.demos

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

private val PhoneBody = Color(0xFF2A3140)
private val PhoneEdge = Color(0xFF8B949E)
private val AxisX = Color(0xFFFF6B6B)
private val AxisY = Color(0xFF5BE49B)
private val Gravity = Color(0xFFFFB74D)

/**
 * A phone seen from the front, turned in 3D by the sensor values: [xOverG] = values[0] / 9.81
 * tips it left/right, [zOverG] = values[2] / 9.81 tips the top away from you (0 = upright,
 * 1 = flat). Red and green arrows are the phone's own x and y axes.
 */
@Composable
fun PhoneModel(xOverG: Float, zOverG: Float, modifier: Modifier = Modifier) {
    val rollDeg = asin(xOverG.coerceIn(-1f, 1f)) * 180f / PI.toFloat()
    val backDeg = asin(zOverG.coerceIn(0f, 1f)) * 180f / PI.toFloat()
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(110.dp, 210.dp)
                .graphicsLayer {
                    rotationY = rollDeg
                    rotationX = backDeg
                    cameraDistance = 10f * density
                }
                .background(PhoneBody, RoundedCornerShape(18.dp))
                .border(2.dp, PhoneEdge, RoundedCornerShape(18.dp))
                .padding(10.dp),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val c = center
                val l = size.minDimension * 0.42f
                drawLine(AxisX, c, c + Offset(l, 0f), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
                drawLine(AxisY, c, c - Offset(0f, l * 1.4f), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(Color(0xFF00E5FF), radius = 6.dp.toPx(), center = c)
            }
        }
    }
}

/**
 * A phone in your hand, seen from the side. It swings from upright to flat and settles at the
 * angle people actually hold it (~38° back), with the live z / 9.81 next to it.
 */
@Composable
fun HoldAngleAnimation(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 8000
                0f at 0 using LinearEasing
                0f at 600 using LinearEasing
                90f at 2600 using LinearEasing
                90f at 3200 using LinearEasing
                38f at 4600 using LinearEasing
                38f at 7200 using LinearEasing
                0f at 8000 using LinearEasing
            },
            repeatMode = RepeatMode.Restart,
        ),
    )
    val measurer = rememberTextMeasurer()
    val label = TextStyle(fontSize = 20.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    Canvas(modifier) {
        val pivot = Offset(size.width * 0.32f, size.height * 0.86f)
        val len = size.height * 0.68f
        val rad = angle * PI.toFloat() / 180f

        // Upright reference.
        drawLine(
            Color.White.copy(alpha = 0.35f), pivot, pivot - Offset(0f, len),
            strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
        )
        // Angle arc.
        drawArc(
            Gravity.copy(alpha = 0.8f), startAngle = -90f, sweepAngle = angle, useCenter = false,
            topLeft = pivot - Offset(len * 0.35f, len * 0.35f), size = Size(len * 0.7f, len * 0.7f),
            style = Stroke(3.dp.toPx()),
        )
        // The phone: tilted back by `angle`, seen edge-on.
        rotate(angle, pivot) {
            drawRoundRect(
                PhoneBody, topLeft = pivot - Offset(9.dp.toPx(), len), size = Size(18.dp.toPx(), len),
                cornerRadius = CornerRadius(8.dp.toPx()),
            )
            drawRoundRect(
                PhoneEdge, topLeft = pivot - Offset(9.dp.toPx(), len), size = Size(18.dp.toPx(), len),
                cornerRadius = CornerRadius(8.dp.toPx()), style = Stroke(2.dp.toPx()),
            )
        }
        // Hand.
        drawCircle(Color(0xFFD9A77F), radius = 22.dp.toPx(), center = pivot + Offset(-6.dp.toPx(), 4.dp.toPx()))

        // Gravity, and the part of it along the screen's normal (z).
        val mid = pivot + Offset(sin(rad) * len * 0.55f, -cos(rad) * len * 0.55f)
        val g = 90.dp.toPx()
        drawLine(Gravity, mid, mid + Offset(0f, g), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        val normal = Offset(-cos(rad), -sin(rad)) // out of the screen, toward the viewer's side
        val zLen = g * sin(rad)
        drawLine(Color(0xFF00E5FF), mid, mid - normal * zLen, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)

        val zOverG = sin(rad)
        val x = size.width * 0.58f
        drawText(measurer, "back %2.0f°".format(angle), Offset(x, size.height * 0.2f), label.copy(color = Gravity))
        drawText(measurer, "z / 9.81 = %.2f".format(zOverG), Offset(x, size.height * 0.2f + 34.dp.toPx()), label.copy(color = Color(0xFF00E5FF)))
        if (angle in 36f..40f) {
            drawText(measurer, "← your hand", Offset(x, size.height * 0.2f + 68.dp.toPx()), label.copy(color = Color.White))
        }
    }
}
