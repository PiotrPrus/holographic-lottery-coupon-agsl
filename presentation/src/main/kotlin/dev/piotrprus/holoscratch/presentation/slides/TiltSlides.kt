package dev.piotrprus.holoscratch.presentation.slides

import androidx.compose.foundation.layout.Arrangement
import kotlin.math.roundToInt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.LaunchedEffect
import dev.piotrprus.holoscratch.presentation.demos.HoldAngleAnimation
import dev.piotrprus.holoscratch.presentation.demos.PhoneModel
import androidx.compose.foundation.layout.fillMaxHeight
import dev.piotrprus.holoscratch.presentation.ui.VideoClip
import dev.piotrprus.holoscratch.presentation.ui.MediaFrame
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.aspectRatio
import kotlin.math.sqrt
import dev.piotrprus.holoscratch.presentation.demos.FoilLook
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.piotrprus.holoscratch.presentation.deck.Slide
import dev.piotrprus.holoscratch.presentation.demos.GravityPhone
import dev.piotrprus.holoscratch.presentation.demos.rememberHandMotion
import dev.piotrprus.holoscratch.presentation.demos.ScratchPanel
import dev.piotrprus.holoscratch.presentation.demos.ScratchState
import dev.piotrprus.holoscratch.presentation.demos.Tilt
import dev.piotrprus.holoscratch.presentation.demos.lean
import dev.piotrprus.holoscratch.presentation.demos.rememberNoisyTilt
import dev.piotrprus.holoscratch.presentation.ui.DeckColors
import dev.piotrprus.holoscratch.presentation.ui.MorphingCodeBlock

// Chapter 3 — "It knows how you hold it". The foil already shines with the mouse; now the phone drives it. What the gravity sensor measures (a picture, not
// trigonometry), the listener built step by step, why it needs smoothing, and the lean.

/**
 * Why we need the sensor, then what it measures. 0 title only · 1 a real foil card tilted in the
 * light (recording) · 2 "we need the phone's position: roll and pitch" · 3 gravity, as a picture ·
 * 4 the phone recording. The reasoning in between is spoken (NOTES.md), not on the slide.
 */
val gravitySlide = Slide(steps = 4) {
    val handMotion = rememberHandMotion()
    LiveSlide(
        title = if (step < 3) "How do you hold it?" else "Gravity sensor",
        subtitle = if (step < 3) "real foil reacts to your hand" else null,
    ) { tilt ->
        if (step < 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(70.dp), verticalAlignment = Alignment.CenterVertically) {
                Reveal(at = 1) {
                    // Two real holo foils in a hand: the coupon and the DevFest sticker.
                    Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                        MediaFrame(Modifier.height(720.dp).aspectRatio(720f / 1280f)) {
                            VideoClip("videos/real_foil_reflections.mp4", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        }
                        MediaFrame(Modifier.height(720.dp).aspectRatio(720f / 1280f)) {
                            VideoClip("videos/real_sticker.mp4", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        }
                    }
                }
                Reveal(at = 2) {
                    Text(
                        "So far the mouse\nmade roll and pitch.\nOn a phone,\nyour hand does.",
                        color = DeckColors.warm,
                        fontSize = 52.sp,
                        lineHeight = 66.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            return@LiveSlide
        }
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.width(560.dp)) {
                Caption("A phone has no \"angle\" sensor.\nIt can only feel gravity.", color = DeckColors.textPrimary)
                Gap(24.dp)
                Caption("How gravity splits over the phone's\nsides tells us roll and pitch.", color = DeckColors.warm)
            }
            // Rolls on its own, like a hand turning the phone; the mouse does nothing here.
            GravityPhone(handMotion.tilt, Modifier.size(620.dp, 720.dp))
            Reveal(at = 4) {
                // A camera recording of the real phone in a hand (9:16), so fit it whole, no crop.
                MediaFrame(Modifier.height(720.dp).aspectRatio(478f / 850f)) {
                    VideoClip("videos/device_tilt.mp4", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                }
            }
        }
    }
}

/** rememberDeviceTilt, built one piece at a time. */
private fun sensorCode(stage: Int) = buildList {
    add("@Composable")
    add("fun rememberDeviceTilt(): State<Tilt> {")
    add("    val tilt = remember { mutableStateOf(Tilt.Zero) }")
    add("    LifecycleResumeEffect(Unit) {")
    if (stage >= 1) add("        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)")
    add("        val listener = object : SensorEventListener {")
    add("            override fun onSensorChanged(event: SensorEvent) {")
    if (stage >= 2) add("                val roll = event.values[0] / 9.81f")
    if (stage == 3) add("                val pitch = event.values[2] / 9.81f")
    if (stage == 4) add("                val pitch = event.values[2] / 9.81f - 0.62f")
    if (stage >= 5) add("                val pitch = (event.values[2] / 9.81f - 0.62f) * 2.2f")
    if (stage >= 2) add("                tilt.value = Tilt(roll, ${if (stage >= 3) "pitch" else "0f"})")
    add("            }")
    add("        }")
    add("        sensorManager.registerListener(listener, sensor, SENSOR_DELAY_GAME)")
    add("        onPauseOrDispose { sensorManager.unregisterListener(listener) }")
    add("    }")
    add("    return tilt")
    add("}")
}.joinToString("\n")

private const val G = 9.81f

/** What the gravity sensor would report for this tilt, held like a phone. See [rawGravity]. */
private data class Gravity(val x: Float, val y: Float, val z: Float)

/**
 * The mouse plays the final roll/pitch; this works backwards to the raw sensor values, so every
 * number on the slide is consistent: x = roll × g, z/g = 0.62 + pitch / 2.2, y takes the rest.
 */
private fun rawGravity(t: Tilt): Gravity {
    val x = t.roll.coerceIn(-1f, 1f) * G
    val z = (0.62f + t.pitch / 2.2f).coerceIn(0f, 1f) * G
    val y = sqrt((G * G - x * x - z * z).coerceAtLeast(0f))
    return Gravity(x, y, z)
}

/**
 * Steps: 0 listener · 1 TYPE_GRAVITY · 2 what values[0..2] are · 3 roll · 4 pitch, raw ·
 * 5 − 0.62: re-centre on the normal hold · 6 × 2.2: tipping as strong as rolling.
 */
val sensorBuildSlide = Slide(steps = 6) {
    val stage = when (step) {
        0, 1 -> step
        2 -> 1 // explaining the values: same code as TYPE_GRAVITY
        else -> step - 1
    }
    val (title, subtitle) = when (step) {
        0 -> "SensorEventListener" to "listen only while the screen is on"
        1 -> "TYPE_GRAVITY" to "already filtered for us"
        2 -> "event.values" to "gravity, split over the phone's own axes"
        3 -> "roll" to "tip it left and right"
        4 -> "pitch" to "tip it toward you"
        5 -> "pitch" to "nobody holds a phone flat"
        else -> "pitch" to "too little room until flat"
    }
    BuildSlide(
        title = title,
        subtitle = subtitle,
        code = sensorCode(stage),
        codeWidth = 1060.dp,
        codeFontSize = 18.sp,
        previewLabel = "Live values",
        previewSize = 600.dp to 360.dp,
        below = {
            when (step) {
                2 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Caption("m/s² along the phone's own axes:\n[0] x right · [1] y up · [2] z out of screen", color = DeckColors.textPrimary)
                    Text(
                        "                 x    y    z\nflat on a table  0.0  0.0  9.8\nupright          0.0  9.8  0.0\nin your hand     0.0  7.7  6.1",
                        color = DeckColors.textSecondary,
                        fontSize = 21.sp,
                        lineHeight = 29.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                3 -> Caption("Divide by gravity → a number from −1 to 1.\nExactly what uTilt expects.")
                4 -> Caption("pitch = z / 9.81\nflat = 1 · upright = 0\nin your hand ≈ 0.62")
                5 -> Caption(
                    "0.62 ≈ how people hold a phone\n(about 38° back from upright).\nSubtract it → normal hold reads 0.",
                    color = DeckColors.warm,
                )
                6 -> Caption(
                    "Rolling sideways goes up to ±1.\nAfter − 0.62, flat is only 0.38:\nthe light barely moves.\n× 2.2 makes tipping as strong as rolling.",
                    color = DeckColors.warm,
                )
                else -> Unit
            }
        },
    ) { tilt ->
        val t = tilt.value
        val raw = rawGravity(t)
        if (step == 5) {
            // The 0.62, as a picture: a phone in a hand, from the side.
            HoldAngleAnimation(Modifier.fillMaxSize())
            return@BuildSlide
        }
        if (step == 6) {
            // The 2.2, as numbers: three ways to hold the phone, before and after the multiplier.
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "              z/g  −0.62  ×2.2\nnormal hold   0.62   0.00  0.00\ntip 20° back  0.85   0.23  0.50\nflat          1.00   0.38  0.84",
                    color = DeckColors.textPrimary,
                    fontSize = 24.sp,
                    lineHeight = 40.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
            return@BuildSlide
        }
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            if (step >= 2) {
                PhoneModel(xOverG = raw.x / G, zOverG = raw.z / G, modifier = Modifier.width(200.dp).fillMaxHeight())
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                if (step >= 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                        SmallValue("x", raw.x)
                        SmallValue("y", raw.y)
                        SmallValue("z", raw.z)
                    }
                    Gap(22.dp)
                }
                BigValue("roll", if (step >= 3) t.roll else null)
                Gap(10.dp)
                BigValue("pitch", if (step >= 4) raw.z / G else null)
            }
        }
    }
}

@Composable
private fun SmallValue(name: String, value: Float) {
    Column {
        Text(name, color = DeckColors.textSecondary, fontSize = 22.sp, fontFamily = FontFamily.Monospace)
        Text("%+.1f".format(value), color = DeckColors.textPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun BigValue(name: String, value: Float?) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(name, color = DeckColors.textSecondary, fontSize = 26.sp, modifier = Modifier.width(100.dp))
        Text(
            if (value == null) "—" else "%+.2f".format(value),
            color = if (value == null) DeckColors.textSecondary else DeckColors.accentAlt,
            fontSize = 46.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

private fun smoothingCode(smooth: Boolean) = if (!smooth) {
    """
    override fun onSensorChanged(event: SensorEvent) {
        roll = event.values[0] / 9.81f
        tilt.value = Tilt(roll, pitch)
    }
    """
} else {
    """
    override fun onSensorChanged(event: SensorEvent) {
        val target = event.values[0] / 9.81f
        roll += (target - roll) * 0.18f   // move 18% of the way
        tilt.value = Tilt(roll, pitch)
    }
    """
}

/**
 * Steps: 0 raw values on a leaning card · 1 why a sensor shakes · 2 one line of smoothing ·
 * 3 why 0.18: too little, just right, too much, side by side.
 */
val smoothingSlide = Slide(steps = 3) {
    LiveSlide(
        title = "Smoothing",
        subtitle = when (step) {
            0 -> "real sensors are shaky"
            1 -> "why is it shaky?"
            2 -> "move part of the way"
            else -> "why 0.18?"
        },
    ) { tilt ->
        if (step < 3) {
            val raw = rememberNoisyTilt(tilt, smoothing = null)
            val smooth = rememberNoisyTilt(tilt, smoothing = 0.18f)
            Row(horizontalArrangement = Arrangement.spacedBy(60.dp)) {
                LabeledDemo("raw", good = false, modifier = Modifier.weight(1f)) {
                    LeaningPanel({ raw.value }, Modifier.fillMaxWidth().height(320.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (step == 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Caption("· Your hand is never still: tiny tremors,\n  many times a second.", color = DeckColors.textPrimary)
                            Caption("· The sensor is a tiny MEMS chip.\n  Every reading carries a little electrical noise.", color = DeckColors.textPrimary)
                            Caption("· It reports ~50 times a second,\n  so we see every wobble.", color = DeckColors.textPrimary)
                        }
                    } else {
                        LabeledDemo("smoothed", good = true, visible = step >= 2) {
                            LeaningPanel({ smooth.value }, Modifier.fillMaxWidth().height(320.dp))
                        }
                    }
                }
            }
            Gap(40.dp)
            if (step >= 2) MorphingCodeBlock(code = smoothingCode(true), fontSize = 28.sp, modifier = Modifier.fillMaxWidth())
            else MorphingCodeBlock(code = smoothingCode(false), fontSize = 28.sp, modifier = Modifier.fillMaxWidth())
        } else {
            val slow = rememberNoisyTilt(tilt, smoothing = 0.01f)
            val middle = rememberNoisyTilt(tilt, smoothing = 0.10f)
            val fast = rememberNoisyTilt(tilt, smoothing = 0.6f)
            Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                LabeledDemo("0.01: smooth, but very late", good = false, modifier = Modifier.weight(1f)) {
                    LeaningPanel({ slow.value }, Modifier.fillMaxWidth().height(260.dp))
                }
                LabeledDemo("0.10", good = true, modifier = Modifier.weight(1f)) {
                    LeaningPanel({ middle.value }, Modifier.fillMaxWidth().height(260.dp))
                }
                LabeledDemo("0.6: fast, still shaky", good = false, modifier = Modifier.weight(1f)) {
                    LeaningPanel({ fast.value }, Modifier.fillMaxWidth().height(260.dp))
                }
            }
            Gap(50.dp)
            Caption(
                "The number = how far each reading moves toward the new value.\n" +
                    "Small: the shake cancels out, but a real tilt takes long to arrive.\n" +
                    "Big: fast, but the shake comes through. The app uses 0.18.",
                color = DeckColors.textPrimary,
            )
        }
    }
}

@Composable
private fun LeaningPanel(tilt: () -> Tilt, modifier: Modifier = Modifier, maxDegrees: Float = 18f, camera: Float = 14f) {
    val scratch = remember { ScratchState() }
    Box(modifier.lean(tilt, maxDegrees, camera)) {
        // The holo foil from the shader chapter, now driven by the sensor.
        ScratchPanel(scratch = scratch, tilt = tilt, look = FoilLook.Holo, scratchable = false, modifier = Modifier.fillMaxSize())
    }
}

private fun leanCode(step: Int, degrees: Float, camera: Float) = buildList {
    add("CouponCard(")
    add("    modifier = Modifier.graphicsLayer {")
    if (step >= 1) {
        add("        val t = tilt()   // roll, pitch: −1..1")
        if (step == 1) {
            add("        rotationY = t.roll")
            add("        rotationX = -t.pitch")
        } else {
            add("        rotationY = t.roll * ${degrees.roundToInt()}f")
            add("        rotationX = -t.pitch * ${degrees.roundToInt()}f")
            add("        cameraDistance = ${camera.roundToInt()}f * density")
        }
    }
    add("    }")
    add(")")
}.joinToString("\n")

/**
 * graphicsLayer lean, from the raw numbers up. 0 flat · 1 raw roll/pitch: rotation is in degrees,
 * so ±1° and nothing moves · 2 multipliers on sliders: try them live · 3 the values the app uses.
 */
val leanSlide = Slide(steps = 3) {
    var degrees by remember { mutableFloatStateOf(1f) }
    var camera by remember { mutableFloatStateOf(8f) }
    LaunchedEffect(step) {
        when (step) {
            2 -> { degrees = 1f; camera = 8f }
            3 -> { degrees = 12f; camera = 14f }
        }
    }
    val (subtitle, caption, good) = when (step) {
        0 -> Triple("lean the card with the phone", "Flat. Just a picture of a card.", null)
        1 -> Triple("start with the raw numbers", "roll and pitch are −1..1.\nrotationY is in degrees: ±1°. Nothing moves.", false)
        2 -> Triple("find the multipliers", "Drag the sliders. Too much? Fisheye?", null)
        else -> Triple("subtle wins", "12° and a far camera: like a card on a table.", true)
    }
    BuildSlide(
        title = "graphicsLayer",
        subtitle = subtitle,
        code = leanCode(step, degrees, camera),
        codeWidth = 900.dp,
        codeFontSize = 28.sp,
        previewSize = 760.dp to 440.dp,
        below = {
            Caption(
                caption,
                color = when (good) {
                    true -> Good
                    false -> Danger
                    null -> DeckColors.textPrimary
                },
            )
            if (step >= 2) {
                Gap(24.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(40.dp), modifier = Modifier.width(760.dp)) {
                    DragSlider("degrees", degrees, 0f..60f, { degrees = it }, Modifier.weight(1f), format = { "%.0f°".format(it) })
                    DragSlider("cameraDistance", camera, 2f..30f, { camera = it }, Modifier.weight(1f))
                }
            }
        },
    ) { tilt ->
        val maxDegrees = when (step) {
            0 -> 0f
            1 -> 1f
            else -> degrees
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LeaningPanel({ tilt.value }, Modifier.width(520.dp).height(300.dp), maxDegrees, if (step >= 2) camera else 8f)
        }
    }
}

val tiltSlides: List<Slide> = listOf(
    chapterSlide("Make it react to your hand"),
    gravitySlide,
    sensorBuildSlide,
    tryItSlide("Gravity sensor", "m3v8ra", note = "iPhone: tap \"Enable motion\" first."),
    smoothingSlide,
    leanSlide,
    tryItSlide("Holo foil", "t9c4wd", note = "Tilt your phone."),
)
