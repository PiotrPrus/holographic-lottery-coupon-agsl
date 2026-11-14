package dev.piotrprus.holoscratch.presentation.slides

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import dev.piotrprus.holoscratch.presentation.demos.MouseTilt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.piotrprus.holoscratch.presentation.deck.Slide
import dev.piotrprus.holoscratch.presentation.demos.FirstShader
import dev.piotrprus.holoscratch.presentation.demos.FirstShaderCanvas
import dev.piotrprus.holoscratch.presentation.demos.HOLO_ALL_LAYERS
import dev.piotrprus.holoscratch.presentation.demos.ScratchPanel
import dev.piotrprus.holoscratch.presentation.demos.ScratchState
import dev.piotrprus.holoscratch.presentation.ui.DeckColors
import dev.piotrprus.holoscratch.presentation.ui.MorphingCodeBlock

// Chapter 2 — "It shines like foil". The mouse drives the light here; the next chapter swaps it for the phone. What a shader is, a first shader in three steps, how it
// plugs into Compose, then the real foil grown one layer at a time — each with its few lines.

val shaderQuestionSlide = questionSlide(
    "What is a shader?",
    "A tiny program that runs once for every pixel —\nall of them at the same time, on the GPU.",
)

/**
 * A first AGSL shader: one colour → a gradient → a light that follows the phone. The light then
 * gets a line-by-line walk-through: what moves it (uTilt), where it sits (light), how it glows.
 */
val firstShaderSlide = Slide(steps = 4) {
    val shader = FirstShader.entries[step.coerceAtMost(2)]
    val (title, subtitle) = when (step) {
        0 -> "AGSL" to "every pixel: \"be pink\""
        1 -> "AGSL" to "every pixel knows where it is"
        2 -> "uniform" to "a value we send from Kotlin"
        3 -> "light" to "where the lamp sits"
        else -> "glow" to "bright near the lamp, dark far away"
    }
    // Line indices in the Spotlight source: 1 uTilt · 5 light · 6 glow · 7 return.
    val highlight = when (step) {
        2 -> setOf(1)
        3 -> setOf(5)
        4 -> setOf(6, 7)
        else -> null
    }
    BuildSlide(
        title = title,
        subtitle = subtitle,
        code = shader.source,
        highlight = highlight,
        codeWidth = 1060.dp,
        codeFontSize = 28.sp,
        previewSize = 580.dp to 400.dp,
        below = { tilt ->
            when (step) {
                0 -> Caption("half4 = red, green, blue, alpha.")
                1 -> Caption("uv goes 0 → 1 across the box.\nRed grows to the right, green grows down.")
                2 -> Caption("uTilt = two numbers from −1 to 1, sent every frame.\nroll: left/right · pitch: up/down.\nFor now, the mouse makes them.", color = DeckColors.warm)
                3 -> Caption(
                    "Mouse in the middle: tilt 0 → light at 0.5, 0.5.\nRoll moves it left/right, pitch up/down.\n" +
                        "uTilt %+.2f %+.2f → light %.2f %.2f".format(tilt.value.roll, tilt.value.pitch, 0.5f + tilt.value.roll * 0.5f, 0.5f - tilt.value.pitch * 0.5f),
                    color = DeckColors.textPrimary,
                )
                else -> Caption("glow = 1 right under the lamp, fading with distance.\n6.0 = how tight the spot is.\n0.35 = the dark grey underneath.", color = DeckColors.textPrimary)
            }
        },
    ) { tilt ->
        FirstShaderCanvas(shader, tilt, Modifier.fillMaxSize())
        if (step >= 3) {
            // Where the shader puts the lamp, so the room can see what `light` is.
            Canvas(Modifier.fillMaxSize()) {
                val t = tilt.value
                val c = Offset(size.width * (0.5f + t.roll * 0.5f), size.height * (0.5f - t.pitch * 0.5f))
                val r = 16.dp.toPx()
                drawLine(DeckColors.warm, c - Offset(r, 0f), c + Offset(r, 0f), strokeWidth = 3.dp.toPx())
                drawLine(DeckColors.warm, c - Offset(0f, r), c + Offset(0f, r), strokeWidth = 3.dp.toPx())
            }
        }
    }
}

/** Plugging the shader into Compose. */
private fun runtimeShaderCode(stage: Int) = buildList {
    add("Modifier.drawWithCache {")
    add("    val shader = RuntimeShader(SPOTLIGHT_AGSL)")
    add("    val brush = ShaderBrush(shader)")
    if (stage >= 1) add("    shader.setFloatUniform(\"uResolution\", size.width, size.height)")
    add("    onDrawWithContent {")
    if (stage >= 2) {
        add("        val t = tilt()")
        add("        shader.setFloatUniform(\"uTilt\", t.roll, t.pitch)")
    }
    add("        drawRect(brush)")
    add("    }")
    add("}")
}.joinToString("\n")

val runtimeShaderSlide = Slide(steps = 2) {
    val (title, subtitle) = when (step) {
        0 -> "RuntimeShader" to "the shader is just a String"
        1 -> "setFloatUniform" to "size: once, in the cache"
        else -> "setFloatUniform" to "tilt: every frame"
    }
    BuildSlide(
        title = title,
        subtitle = subtitle,
        code = runtimeShaderCode(step.coerceAtMost(2)),
        codeWidth = 1060.dp,
        codeFontSize = 27.sp,
        previewSize = 580.dp to 400.dp,
        below = {
            when (step) {
                0 -> Caption("Compiled once — drawWithCache keeps it.")
                2 -> Caption("Read in the draw phase:\nthe mouse moves, nothing recomposes.")
                else -> Unit
            }
        },
    ) { tilt ->
        FirstShaderCanvas(FirstShader.Spotlight, tilt, Modifier.fillMaxSize())
    }
}

/** The real foil, as the shader grows. Each layer adds a few lines. */
private fun foilCode(layers: Int) = buildList {
    add("half4 main(float2 fragCoord) {")
    add("    float3 col = float3(0.58, 0.59, 0.62);              // aluminium grey")
    if (layers >= 1) {
        add("    float grain = noise(float2(p.x * 5.0, p.y * 1100.0)); // stretched 200×")
        add("    col += (grain - 0.5) * 0.11;")
    }
    if (layers >= 2) {
        add("    float2 light = float2(0.5 + roll, 0.5 - pitch);      // a lamp that follows tilt")
        add("    col += exp(-dot(p - light, p - light) * 7.0) * 0.16;")
    }
    if (layers >= 3) {
        add("    float band = exp(-pow((p.y - light.y) * 2.6, 2.0));  // where the light hits")
        add("    float phase = p.y * 1.8 + roll * 0.6 - pitch * 0.9;")
        add("    col = mix(col, rainbow(phase), band * 0.35);")
    }
    if (layers >= 4) {
        add("    float2 cell = floor(p * 170.0);                      // tiny mirrors")
        add("    float angle = hash(cell) * 2.0 * PI;                 // each tilted randomly")
        add("    col += step(0.96, cos(angle + roll * 7.0)) * 0.5;    // flash when aligned")
    }
    if (layers >= 5) {
        add("    col += exp(-dot(p - light, p - light) * 1.4) * 0.06; // plastic glare")
        add("    col *= edgeShade(uv);")
    }
    add("    return half4(col, 1.0);")
    add("}")
}.joinToString("\n")

private val layerTitles = listOf(
    "grey" to "where everyone starts",
    "brushed grain" to "noise, stretched sideways",
    "a lamp" to "the reflection follows the mouse",
    "the rainbow" to "only where the light hits",
    "glitter" to "tiny mirrors that flash",
    "glare" to "the plastic on top",
)

/** The foil, built live one layer at a time. Move the mouse on every step. */
val foilBuildSlide = Slide(steps = HOLO_ALL_LAYERS) {
    val (title, subtitle) = layerTitles[step]
    BuildSlide(
        title = title,
        subtitle = subtitle,
        code = foilCode(step),
        codeWidth = 1160.dp,
        codeFontSize = 21.sp,
        previewSize = 500.dp to 360.dp,
    ) { tilt ->
        val scratch = remember { ScratchState() }
        ScratchPanel(scratch, { tilt.value }, layers = step, scratchable = false, prizeScale = 0.6f, modifier = Modifier.fillMaxSize())
    }
}

private const val RAINBOW_CODE = """
float phase = p.y * 1.8 + roll * 0.6 - pitch * 0.9;
col = mix(col, rainbow(phase), band * 0.35);
"""

/**
 * Rainbow phase, all on one slide: the code, the colour wheel unrolled (one rainbow per 1.0),
 * and two previews driven by the same mouse: only rainbow(phase), and the foil it ends up on.
 */
val oilSlickSlide = Slide {
    LiveSlide(title = "Rainbow phase", subtitle = "phase picks the colour") { tilt ->
        val foil = remember { ScratchState() }
        Row(horizontalArrangement = Arrangement.spacedBy(50.dp)) {
            Column(Modifier.weight(1.1f)) {
                MorphingCodeBlock(code = RAINBOW_CODE, fontSize = 24.sp, modifier = Modifier.fillMaxWidth())
                Gap(30.dp)
                PhaseRuler(Modifier.fillMaxWidth().height(110.dp))
                Gap(24.dp)
                Caption(
                    "phase is a position on the colour wheel:\n0 = red · 0.5 = cyan · 1.0 = red again.\n" +
                        "p.y * 1.8: top to bottom = 1.8 rainbows.\n" +
                        "roll * 0.6: tilting slides the stripes.\n" +
                        "band keeps the colour only near the light.",
                    color = DeckColors.textPrimary,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(30.dp)) {
                LabeledDemo("only rainbow(phase)") {
                    RainbowStripes(tilt, rollGain = 0.6f, pitchGain = 0.9f, Modifier.fillMaxWidth().height(260.dp))
                }
                LabeledDemo("on the foil") {
                    ScratchPanel(foil, { tilt.value }, scratchable = false, modifier = Modifier.fillMaxWidth().height(260.dp))
                }
            }
        }
    }
}

/** The card painted with nothing but rainbow(phase): horizontal stripes that slide with the mouse. */
@Composable
private fun RainbowStripes(tilt: MouseTilt, rollGain: Float, pitchGain: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(22.dp))) {
        val t = tilt.value
        val rows = 160
        val h = size.height / rows
        for (i in 0 until rows) {
            val py = (i + 0.5f) / rows
            val phase = py * 1.8f + t.roll * rollGain - t.pitch * pitchGain
            drawRect(spectrumColor(phase, vivid = true), topLeft = Offset(0f, i * h), size = Size(size.width, h + 1f))
        }
    }
}

/** The colour wheel unrolled: phase 0 → 2, with the numbers under it. One rainbow per 1.0. */
@Composable
private fun PhaseRuler(modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        val span = 2f
        val barH = size.height * 0.45f
        val steps = 200
        val w = size.width / steps
        for (i in 0 until steps) {
            drawRect(spectrumColor(i / steps.toFloat() * span, vivid = true), topLeft = Offset(i * w, 0f), size = Size(w + 1f, barH))
        }
        listOf(0f, 0.5f, 1f, 1.5f, 2f).forEach { v ->
            val x = (size.width * v / span).coerceIn(2f, size.width - 2f)
            drawLine(Color.White.copy(alpha = 0.6f), Offset(x, barH), Offset(x, barH + 10.dp.toPx()), strokeWidth = 2.dp.toPx())
            val label = if (v % 1f == 0f) "%.0f".format(v) else "%.1f".format(v)
            val layout = measurer.measure(label, TextStyle(color = Color.White, fontSize = 22.sp, fontFamily = FontFamily.Monospace))
            drawText(layout, topLeft = Offset((x - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width), barH + 16.dp.toPx()))
        }
    }
}

/** The shader's spectrum(): a cosine palette, mixed toward grey. */
private fun spectrumColor(t: Float, vivid: Boolean = false): Color {
    fun ch(o: Float) = 0.5f + 0.5f * cos(2f * PI.toFloat() * (t + o))
    fun mix(c: Float) = if (vivid) c else 0.5f + (c - 0.5f) * 0.55f
    return Color(mix(ch(0f)), mix(ch(0.333f)), mix(ch(0.667f)))
}

/** The same card code before and after: one line removed, one line changed. */
private fun sheenCode(after: Boolean) = buildList {
    add("Box {")
    add("    Prize()")
    add("    Foil(scratch)")
    if (!after) add("    Shine()   // own layer, on top")
    add("}")
    add("")
    add("// inside Foil (its own offscreen layer)")
    add(".drawWithCache {")
    add("    onDrawWithContent {")
    add(if (after) "        drawRect(holoBrush)   // the shine IS the foil" else "        drawRect(Silver)")
    add("        drawPath(path, ..., blendMode = BlendMode.Clear)")
    add("    }")
    add("}")
}.joinToString("\n")

/**
 * Where the shine lives, problem first. 0 the bad card alone: scratch it, the rainbow stays on bare
 * paper · 1 the code: Shine() is its own layer on top, Clear inside Foil can't touch it · 2 the
 * code morphs: Shine() is gone, Foil paints holoBrush instead of Silver · 3 the fixed card appears
 * under it, same finger on both.
 */
val sheenSlide = Slide(steps = 3) {
    LiveSlide(
        title = "Where does the shine live?",
        subtitle = when (step) {
            0 -> null
            1 -> "a layer on top of the foil"
            else -> "the foil itself shines"
        },
    ) { tilt ->
        val inside = remember { ScratchState() }
        val overlay = remember { ScratchState() }
        Row(horizontalArrangement = Arrangement.spacedBy(50.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                LabeledDemo("shine painted on top", good = if (step >= 1) false else null, showLabel = step >= 1) {
                    ScratchPanel(overlay, { tilt.value }, mirrors = listOf(inside), sheenInside = false, modifier = Modifier.fillMaxWidth().height(250.dp))
                }
                LabeledDemo("shine inside the foil", good = true, visible = step >= 3) {
                    ScratchPanel(inside, { tilt.value }, mirrors = listOf(overlay), scratchable = step >= 3, modifier = Modifier.fillMaxWidth().height(250.dp))
                }
                ResetButton({ inside.reset(); overlay.reset() })
            }
            Column(Modifier.weight(1.1f)) {
                Reveal(at = 1) {
                    Column {
                        MorphingCodeBlock(code = sheenCode(after = step >= 2), fontSize = 22.sp, modifier = Modifier.fillMaxWidth())
                        Gap(24.dp)
                        if (step >= 2) {
                            Caption("Shine() is gone. Foil paints holoBrush instead of Silver.\nSame layer → Clear takes metal and rainbow.", color = Good)
                        } else {
                            Caption("Clear runs inside Foil's layer.\nShine() is a different layer: it never gets erased.", color = Danger)
                        }
                    }
                }
            }
        }
    }
}

val shineSlides: List<Slide> = listOf(
    chapterSlide("Make it shine", "from one pink box to holo foil"),
    shaderQuestionSlide,
    firstShaderSlide,
    runtimeShaderSlide,
    foilBuildSlide,
    oilSlickSlide,
    sheenSlide,
    bookOfShadersSlide,
)
