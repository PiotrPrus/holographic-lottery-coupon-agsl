package dev.piotrprus.holoscratch.presentation.demos

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeShader
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Small, self-contained demos used to *teach* one idea each: a first shader, what the gravity
// sensor measures, why sensor values need smoothing.

// ── first shaders ──────────────────────────────────────────────────────────

/** The first-shader progression. Each source is shown on the slide exactly as it runs. */
enum class FirstShader(val source: String) {
    Solid(
        """
        half4 main(float2 fragCoord) {
            return half4(1.0, 0.3, 0.6, 1.0);
        }
        """.trimIndent(),
    ),
    Gradient(
        """
        uniform float2 uResolution;

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / uResolution;
            return half4(uv.x, uv.y, 1.0, 1.0);
        }
        """.trimIndent(),
    ),
    Spotlight(
        """
        uniform float2 uResolution;
        uniform float2 uTilt;

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / uResolution;
            float2 light = 0.5 + float2(uTilt.x, -uTilt.y) * 0.5;
            float glow = exp(-distance(uv, light) * 6.0);
            return half4(half3(0.35 + glow), 1.0);
        }
        """.trimIndent(),
    ),
}

private val firstEffects = mutableMapOf<FirstShader, RuntimeEffect>()

fun firstShaderBrush(shader: FirstShader, size: Size, tilt: Tilt): ShaderBrush {
    val effect = firstEffects.getOrPut(shader) { RuntimeEffect.makeForShader(shader.source) }
    val builder = RuntimeShaderBuilder(effect)
    if (shader != FirstShader.Solid) builder.uniform("uResolution", size.width, size.height)
    if (shader == FirstShader.Spotlight) builder.uniform("uTilt", tilt.roll, tilt.pitch)
    return ShaderBrush(builder.makeShader().asComposeShader())
}

@Composable
fun FirstShaderCanvas(shader: FirstShader, tilt: MouseTilt, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawRect(firstShaderBrush(shader, size, tilt.value)) }
}

// ── gravity, explained ─────────────────────────────────────────────────────

private const val G = 9.81f

/**
 * A phone seen from the front, turned by the mouse. Gravity always points straight down; the
 * sensor reports how much of it falls along the phone's own X and Y axes. That split is the
 * whole idea behind `values[0]` and `values[1]`.
 */
@Composable
fun GravityPhone(tilt: MouseTilt, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val label = TextStyle(fontSize = 26.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    Canvas(modifier) {
        val angleDeg = tilt.value.roll * 60f
        val angle = angleDeg * PI.toFloat() / 180f
        val c = center
        val phoneH = size.height * 0.62f
        val phoneW = phoneH * 0.5f
        val gx = sin(angle) * G // gravity along the phone's X axis
        val gy = cos(angle) * G // gravity along the phone's Y axis
        val scale = phoneH * 0.035f

        rotate(angleDeg, c) {
            drawRoundRect(
                color = Color(0xFF2A3140),
                topLeft = Offset(c.x - phoneW / 2, c.y - phoneH / 2),
                size = Size(phoneW, phoneH),
                cornerRadius = CornerRadius(phoneW * 0.14f),
            )
            drawRoundRect(
                color = Color(0xFF8B949E),
                topLeft = Offset(c.x - phoneW / 2, c.y - phoneH / 2),
                size = Size(phoneW, phoneH),
                cornerRadius = CornerRadius(phoneW * 0.14f),
                style = Stroke(width = 4f),
            )
            // Components, drawn in the phone's own frame.
            drawLine(Color(0xFFFF6B6B), c, Offset(c.x + gx * scale, c.y), strokeWidth = 10f, cap = StrokeCap.Round)
            drawLine(Color(0xFF5BE49B), c, Offset(c.x, c.y + gy * scale), strokeWidth = 10f, cap = StrokeCap.Round)
        }
        // Gravity itself: always down, whatever the phone does.
        drawLine(Color(0xFFFFB74D), c, Offset(c.x, c.y + G * scale), strokeWidth = 6f, cap = StrokeCap.Round)

        drawText(measurer, "gravity ↓", Offset(c.x + 24f, c.y + G * scale - 16f), label.copy(color = Color(0xFFFFB74D)))
        drawText(measurer, "values[0] = %+.1f".format(gx), Offset(20f, size.height - 110f), label.copy(color = Color(0xFFFF6B6B)))
        drawText(measurer, "values[1] = %+.1f".format(gy), Offset(20f, size.height - 60f), label.copy(color = Color(0xFF5BE49B)))
    }
}

// ── noisy sensors ──────────────────────────────────────────────────────────

/**
 * The mouse tilt with sensor-like noise added every frame. With [smoothing] null the noise goes
 * straight through; otherwise it is smoothed exactly like the app does it.
 */
@Composable
fun rememberNoisyTilt(source: MouseTilt, smoothing: Float?): State<Tilt> {
    val state = remember { mutableStateOf(Tilt.Render) }
    if (!LocalInspectionMode.current) {
        LaunchedEffect(source, smoothing) {
            while (true) {
                withFrameNanos {
                    val t = source.target
                    val raw = Tilt(
                        roll = t.roll + (Random.nextFloat() - 0.5f) * 0.35f,
                        pitch = t.pitch + (Random.nextFloat() - 0.5f) * 0.35f,
                    )
                    state.value = if (smoothing == null) {
                        raw
                    } else {
                        val cur = state.value
                        Tilt(
                            roll = cur.roll + (raw.roll - cur.roll) * smoothing,
                            pitch = cur.pitch + (raw.pitch - cur.pitch) * smoothing,
                        )
                    }
                }
            }
        }
    }
    return state
}
