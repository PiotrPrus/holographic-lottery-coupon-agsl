package dev.piotrprus.holoscratch.presentation.demos

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.piotrprus.particleemitter.CanvasEmitterConfig
import dev.piotrprus.particleemitter.CanvasParticleEmitter
import dev.piotrprus.particleemitter.ParticleShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Showcase effects from the ParticleEmitter talk (ParticleEmitter repo, `presentation` branch),
// sized to whatever box they sit in. Decorative and ambient: nothing here reacts to input.

/** Campfire: a wide red-orange body, a hot yellow core and a few stray sparks, all additive. */
@Composable
fun FlameDemo(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val base = DpOffset(maxWidth / 2, maxHeight)
        CanvasParticleEmitter(
            modifier = Modifier.fillMaxSize(),
            config = CanvasEmitterConfig(
                particlePerSecond = 600,
                emitterCenter = base,
                startRegionShape = CanvasEmitterConfig.Shape.H_LINE,
                startRegionSize = DpSize(90.dp, 0.dp),
                particleShapes = listOf(ParticleShape.Circle),
                lifespanRange = 900..1600,
                fadeOutTime = 700..1200,
                scaleTime = 700..1300,
                colors = listOf(Color(0xFFFF5722), Color(0xFFE64A19), Color(0xFFFF7043)),
                particleSizes = listOf(DpSize(8.dp, 8.dp), DpSize(11.dp, 11.dp), DpSize(6.dp, 6.dp)),
                spread = IntRange(-10, 10),
                blendMode = BlendMode.Plus,
                initialForce = IntRange(90, 200),
                startScaleRange = IntRange(1, 1),
                targetScaleRange = IntRange(0, 0),
                gravityStrength = 60f,
                gravityAngle = 180,
            ),
        )
        CanvasParticleEmitter(
            modifier = Modifier.fillMaxSize(),
            config = CanvasEmitterConfig(
                particlePerSecond = 450,
                emitterCenter = base,
                startRegionShape = CanvasEmitterConfig.Shape.H_LINE,
                startRegionSize = DpSize(40.dp, 0.dp),
                particleShapes = listOf(ParticleShape.Circle),
                lifespanRange = 600..1100,
                fadeOutTime = 450..800,
                scaleTime = 450..900,
                colors = listOf(Color(0xFFFFC107), Color(0xFFFFE082), Color(0xFFFFF8E1)),
                particleSizes = listOf(DpSize(5.dp, 5.dp), DpSize(7.dp, 7.dp)),
                spread = IntRange(-6, 6),
                blendMode = BlendMode.Plus,
                initialForce = IntRange(130, 240),
                startScaleRange = IntRange(1, 1),
                targetScaleRange = IntRange(0, 0),
                gravityStrength = 80f,
                gravityAngle = 180,
            ),
        )
        CanvasParticleEmitter(
            modifier = Modifier.fillMaxSize(),
            config = CanvasEmitterConfig(
                particlePerSecond = 12,
                emitterCenter = DpOffset(maxWidth / 2, maxHeight - 20.dp),
                startRegionShape = CanvasEmitterConfig.Shape.H_LINE,
                startRegionSize = DpSize(60.dp, 0.dp),
                particleShapes = listOf(ParticleShape.Circle),
                lifespanRange = 1400..2400,
                fadeOutTime = 1000..1800,
                scaleTime = 300..500,
                colors = listOf(Color(0xFFFFD54F), Color(0xFFFFAB40)),
                particleSizes = listOf(DpSize(3.dp, 3.dp), DpSize(4.dp, 4.dp)),
                spread = IntRange(-25, 25),
                blendMode = BlendMode.Plus,
                initialForce = IntRange(180, 320),
                startScaleRange = IntRange(1, 1),
                targetScaleRange = IntRange(0, 1),
                gravityStrength = 50f,
                gravityAngle = 180,
            ),
        )
    }
}

private val MatrixGlyphs = listOf("ア", "カ", "ネ", "ク", "ヲ", "ミ", "7", "0", "1", "3")
private val MatrixGreens = listOf(Color(0xFF00FF41), Color(0xFF00C838), Color(0xFFCCFFCC))
private val MatrixFaintGreens = listOf(Color(0xFF0A5F1E), Color(0xFF0E8226))

// ParticleShape.Text ignores colors and particleSizes: the glyph is rasterized once from its
// TextStyle, so colour and size are baked in here.
private fun matrixGlyphShapes(measurer: TextMeasurer, palette: List<Color>, fontSizes: List<Int>): List<ParticleShape> =
    MatrixGlyphs.mapIndexed { index, glyph ->
        ParticleShape.Text(
            text = glyph,
            textStyle = TextStyle(
                fontSize = fontSizes[index % fontSizes.size].sp,
                color = palette[index % palette.size],
                fontWeight = FontWeight.Bold,
            ),
            textMeasurer = measurer,
        )
    }

/** Digital rain: glyphs falling with additive glow, a bright near layer over a faint far one. */
@Composable
fun MatrixRainDemo(modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val far = remember(measurer) { matrixGlyphShapes(measurer, MatrixFaintGreens, listOf(12, 15, 18)) }
    val near = remember(measurer) { matrixGlyphShapes(measurer, MatrixGreens, listOf(17, 20, 24)) }
    BoxWithConstraints(modifier) {
        val top = DpOffset(maxWidth / 2, 0.dp)
        val line = DpSize(maxWidth, 0.dp)
        CanvasParticleEmitter(
            modifier = Modifier.fillMaxSize(),
            config = CanvasEmitterConfig(
                particlePerSecond = 120,
                emitterCenter = top,
                startRegionShape = CanvasEmitterConfig.Shape.H_LINE,
                startRegionSize = line,
                particleShapes = far,
                lifespanRange = 3500..5000,
                fadeOutTime = 3000..4500,
                scaleTime = 100..200,
                colors = MatrixFaintGreens,
                particleSizes = listOf(DpSize(12.dp, 12.dp), DpSize(15.dp, 15.dp)),
                spread = IntRange(180, 180),
                blendMode = BlendMode.Plus,
                initialForce = IntRange(60, 180),
                rotationRange = IntRange(0, 0),
                startScaleRange = IntRange(1, 1),
                targetScaleRange = IntRange(1, 1),
                gravityStrength = 30f,
                gravityAngle = 0,
            ),
        )
        CanvasParticleEmitter(
            modifier = Modifier.fillMaxSize(),
            config = CanvasEmitterConfig(
                particlePerSecond = 160,
                emitterCenter = top,
                startRegionShape = CanvasEmitterConfig.Shape.H_LINE,
                startRegionSize = line,
                particleShapes = near,
                lifespanRange = 1800..3200,
                fadeOutTime = 1500..2800,
                scaleTime = 100..200,
                colors = MatrixGreens,
                particleSizes = listOf(DpSize(18.dp, 18.dp), DpSize(22.dp, 22.dp), DpSize(26.dp, 26.dp)),
                spread = IntRange(180, 180),
                blendMode = BlendMode.Plus,
                initialForce = IntRange(140, 420),
                rotationRange = IntRange(0, 0),
                startScaleRange = IntRange(1, 1),
                targetScaleRange = IntRange(1, 1),
                gravityStrength = 60f,
                gravityAngle = 0,
            ),
        )
    }
}

private val WandGold = Color(0xFFFFC93C)
private val WandStick = Color(0xFF6B4A2B)
private val MagicColors = listOf(Color(0xFFFFFFFF), Color(0xFFFFE066), Color(0xFFFFC93C), Color(0xFFB3E0FF))

/** The magic wand from the samples: it wanders on a Lissajous curve and trails stars. */
@Composable
fun MagicWandDemo(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    var box by remember { mutableStateOf(IntSize.Zero) }
    var time by remember { mutableFloatStateOf(1.2f) }
    if (!LocalInspectionMode.current) {
        LaunchedEffect(Unit) {
            var last = 0L
            while (true) {
                withFrameNanos { now ->
                    if (last != 0L) time += (now - last) / 1_000_000_000f
                    last = now
                }
            }
        }
    }
    val stars = remember(density) {
        listOf(8.dp, 11.dp, 14.dp).map { size ->
            val r = with(density) { size.toPx() } / 2f
            ParticleShape.PathShape(starPath(r, centerOffsetPx = r / 3f))
        }
    }
    val tip = remember(density) { starPath(with(density) { 14.dp.toPx() }, centerOffsetPx = 0f) }
    val stickLen = with(density) { 70.dp.toPx() }
    val stickWidth = with(density) { 4.dp.toPx() }

    Box(modifier.onSizeChanged { box = it }) {
        if (box != IntSize.Zero) {
            val wand = Offset(
                x = box.width * (0.5f + 0.34f * sin(1.1f * time)),
                y = box.height * (0.42f + 0.30f * sin(0.7f * time + 1.3f)),
            )
            CanvasParticleEmitter(
                modifier = Modifier.fillMaxSize(),
                config = CanvasEmitterConfig(
                    particlePerSecond = 110,
                    emitterCenter = with(density) { DpOffset(wand.x.toDp(), wand.y.toDp()) },
                    startRegionShape = CanvasEmitterConfig.Shape.POINT,
                    startRegionSize = DpSize.Zero,
                    particleShapes = stars,
                    lifespanRange = 1400..2400,
                    fadeOutTime = 1000..1800,
                    scaleTime = 1400..2400,
                    colors = MagicColors,
                    particleSizes = listOf(DpSize(8.dp, 8.dp), DpSize(11.dp, 11.dp), DpSize(14.dp, 14.dp)),
                    spread = IntRange(0, 360),
                    scaleEasing = EaseOutCubic,
                    alphaEasing = EaseOutCubic,
                    initialForce = IntRange(20, 45),
                    startScaleRange = IntRange(1, 1),
                    targetScaleRange = IntRange(0, 0),
                    rotationRange = IntRange(-120, 120),
                    gravityStrength = 90f,
                    gravityAngle = 0,
                ),
            )
            Canvas(Modifier.fillMaxSize()) {
                rotate(degrees = 25f, pivot = wand) {
                    drawRect(WandStick, topLeft = Offset(wand.x - stickWidth / 2f, wand.y), size = Size(stickWidth, stickLen))
                }
                translate(left = wand.x, top = wand.y) { drawPath(tip, WandGold) }
            }
        }
    }
}

private fun starPath(radiusPx: Float, centerOffsetPx: Float): Path {
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) radiusPx else radiusPx * 0.45f
        val angle = -PI / 2 + i * PI / 5
        val x = centerOffsetPx + r * cos(angle).toFloat()
        val y = centerOffsetPx + r * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
