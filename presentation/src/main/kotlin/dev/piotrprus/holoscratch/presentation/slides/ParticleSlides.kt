package dev.piotrprus.holoscratch.presentation.slides

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.piotrprus.holoscratch.presentation.deck.Slide
import dev.piotrprus.holoscratch.presentation.demos.FlameDemo
import dev.piotrprus.holoscratch.presentation.demos.MagicWandDemo
import dev.piotrprus.holoscratch.presentation.demos.MatrixRainDemo
import dev.piotrprus.holoscratch.presentation.ui.DeckColors
import dev.piotrprus.particleemitter.CanvasEmitterConfig
import dev.piotrprus.particleemitter.CanvasParticleEmitter
import dev.piotrprus.particleemitter.ParticleShape
import io.github.alexzhirkevich.qrose.rememberQrCodePainter

// The particle library on its own, before it goes anywhere near the scratch card: what it can do,
// where to get it, and its parameters one at a time.

const val PARTICLE_REPO_URL = "https://github.com/PiotrPrus/ParticleEmitter"

private val ShowcaseBackground = Color(0xFF0A0D12)

/** Three effects from the particle talk, live. One click adds the QR code to the repo. */
val particleShowcaseSlide = Slide(steps = 1) {
    LiveSlide(title = "ParticleEmitter", subtitle = "my particle library for Compose Multiplatform") {
        Row(horizontalArrangement = Arrangement.spacedBy(36.dp), verticalAlignment = Alignment.Top) {
            ShowcaseCard("matrix") { MatrixRainDemo(Modifier.fillMaxSize()) }
            ShowcaseCard("fire") { FlameDemo(Modifier.fillMaxSize()) }
            ShowcaseCard("magic wand") { MagicWandDemo(Modifier.fillMaxSize()) }
            Reveal(at = 1) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(280.dp)
                            .background(Color.White, RoundedCornerShape(18.dp))
                            .padding(22.dp),
                    ) {
                        Image(
                            painter = rememberQrCodePainter(PARTICLE_REPO_URL),
                            contentDescription = "QR code to the ParticleEmitter repository",
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Gap(20.dp)
                    Text("github.com/PiotrPrus/\nParticleEmitter", color = DeckColors.textSecondary, fontSize = 24.sp, lineHeight = 32.sp)
                }
            }
        }
    }
}

@Composable
private fun ShowcaseCard(label: String, content: @Composable () -> Unit) {
    Column {
        DemoLabel(label, null)
        Gap(18.dp)
        Box(
            Modifier
                .width(400.dp)
                .height(560.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(ShowcaseBackground)
                .border(1.dp, DeckColors.codeBorder, RoundedCornerShape(24.dp)),
        ) { content() }
    }
}

/** The config as it grows on the parameters slide: two lines, then one idea per click. */
private fun paramsCode(stage: Int) = buildList {
    add("CanvasParticleEmitter(")
    add("    config = CanvasEmitterConfig(")
    add("        particlePerSecond = 200,")
    add("        emitterCenter = pointer,")
    if (stage >= 1) {
        add("        colors = listOf(Orange, Pink, Cyan),")
        add("        particleSizes = listOf(6.dp, 10.dp),")
    }
    if (stage >= 2) {
        add("        spread = -30..30,           // 0° = up")
        add("        initialForce = 250..450,    // launch speed")
    }
    if (stage >= 3) {
        add("        lifespanRange = 800..1600,  // ms")
        add("        fadeOutTime = 600..1200,")
    }
    if (stage >= 4) {
        add("        gravityStrength = 600f,")
        add("        gravityAngle = 0,           // 0 = down")
    }
    add("    ),")
    add(")")
}.joinToString("\n")

private val ParamOrange = Color(0xFFFF9F43)
private val ParamPink = Color(0xFFFF5FA2)
private val ParamCyan = Color(0xFF3DDCFF)

/**
 * CanvasEmitterConfig, one idea per click, on a plain dark stage. The emitter sits wherever the
 * pointer is over the preview. 0 rate + position · 1 colours, sizes · 2 spread, force ·
 * 3 lifespan, fade · 4 gravity.
 */
val emitterParamsSlide = Slide(steps = 4) {
    val (title, subtitle) = when (step) {
        0 -> "CanvasEmitterConfig" to "how many, and from where"
        1 -> "colors" to "each particle picks one"
        2 -> "spread" to "which way, how hard"
        3 -> "lifespanRange" to "how long each one lives"
        else -> "gravityStrength" to "what pulls them"
    }
    BuildSlide(
        title = title,
        subtitle = subtitle,
        code = paramsCode(step),
        codeWidth = 960.dp,
        codeFontSize = 26.sp,
        previewSize = 700.dp to 520.dp,
        below = {
            Caption(
                when (step) {
                    0 -> "particlePerSecond: how many are born.\nemitterCenter: where. Here, the pointer."
                    1 -> "A random colour and size from the lists."
                    2 -> "spread: a range of angles, in degrees.\ninitialForce: a range of launch speeds."
                    3 -> "Each particle lives 0.8–1.6 s,\nfading out over its last moments."
                    else -> "A constant pull. gravityAngle 0 = down.\nThe fountain falls back."
                },
                color = DeckColors.textPrimary,
            )
        },
    ) {
        ParamsPreview(step, Modifier.fillMaxSize())
    }
}

@Composable
private fun ParamsPreview(stage: Int, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    var center by remember { mutableStateOf(DpOffset.Unspecified) }
    var boxSize by remember { mutableStateOf(DpSize.Zero) }
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ShowcaseBackground)
            .onSizeChanged { with(density) { boxSize = DpSize(it.width.toDp(), it.height.toDp()) } }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val p = event.changes.firstOrNull()?.position ?: continue
                        center = if (event.type == PointerEventType.Exit) DpOffset.Unspecified
                        else with(density) { DpOffset(p.x.toDp(), p.y.toDp()) }
                    }
                }
            },
    ) {
        // Without a pointer the emitter waits in the lower middle.
        val at = if (center == DpOffset.Unspecified) DpOffset(boxSize.width / 2, boxSize.height * 0.7f) else center
        CanvasParticleEmitter(
            modifier = Modifier.fillMaxSize(),
            config = paramsConfig(stage, at),
        )
    }
}

/**
 * Everything the slide hasn't shown yet stays plain: white dots, thrown gently in every direction,
 * no gravity. Each click swaps one group for the values on screen.
 */
private fun paramsConfig(stage: Int, at: DpOffset) = CanvasEmitterConfig(
    particlePerSecond = 200,
    emitterCenter = at,
    startRegionShape = CanvasEmitterConfig.Shape.POINT,
    startRegionSize = DpSize.Zero,
    particleShapes = listOf(ParticleShape.Circle),
    lifespanRange = if (stage >= 3) 800..1600 else 2500..3000,
    fadeOutTime = if (stage >= 3) 600..1200 else 2400..2900,
    scaleTime = 200..400,
    colors = if (stage >= 1) listOf(ParamOrange, ParamPink, ParamCyan) else listOf(Color.White),
    particleSizes = if (stage >= 1) listOf(DpSize(6.dp, 6.dp), DpSize(10.dp, 10.dp)) else listOf(DpSize(6.dp, 6.dp)),
    spread = if (stage >= 2) -30..30 else -180..180,
    initialForce = if (stage >= 2) 250..450 else 40..90,
    rotationRange = 0..0,
    startScaleRange = 1..1,
    targetScaleRange = 1..1,
    gravityStrength = if (stage >= 4) 600f else 0f,
    gravityAngle = 0,
)
