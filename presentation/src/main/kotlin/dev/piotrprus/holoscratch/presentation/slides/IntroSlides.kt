package dev.piotrprus.holoscratch.presentation.slides

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.piotrprus.holoscratch.presentation.deck.Slide
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import com.gsrathoreniks.scratchify.api.Scratchify
import com.gsrathoreniks.scratchify.api.ScratchifyController
import com.gsrathoreniks.scratchify.api.config.BrushShape
import com.gsrathoreniks.scratchify.api.config.ScratchifyBrushConfig
import com.gsrathoreniks.scratchify.api.config.ScratchifyConfig
import dev.piotrprus.holoscratch.presentation.demos.FlatFoil
import dev.piotrprus.holoscratch.presentation.demos.PrintArea
import dev.piotrprus.holoscratch.presentation.demos.Prize
import dev.piotrprus.holoscratch.presentation.demos.handHeld
import dev.piotrprus.holoscratch.presentation.demos.rememberHandMotion
import dev.piotrprus.holoscratch.presentation.ui.DeckColors
import dev.piotrprus.holoscratch.presentation.ui.MediaFrame
import dev.piotrprus.holoscratch.presentation.ui.PhotoImage
import dev.piotrprus.holoscratch.presentation.ui.VideoClip

const val TALK_TITLE = "Scratch to Reveal"
const val TALK_SUBTITLE = "Building a Holographic Card with AGSL, Sensors and Particles"

/**
 * Title: the talk name is printed in holo foil. It moves on its own, as if someone holds it and
 * turns it in the light: it rolls, pitches and lifts, and the foil shine follows.
 */
val titleSlide = Slide {
    val motion = rememberHandMotion()
    Box(Modifier.fillMaxSize().background(StageBackground)) {
        Column(Modifier.align(Alignment.CenterStart).padding(start = 110.dp, bottom = 160.dp)) {
            Box {
                // Shadow stays on the ground: it softens and fades as the title lifts.
                Text(
                    text = TALK_TITLE,
                    fontSize = 150.sp,
                    lineHeight = 150.sp * 1.15f,
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    modifier = Modifier
                        .graphicsLayer {
                            val lift = motion.lift
                            alpha = 0.45f - lift * 0.12f
                            translationY = (10f + lift * 6f) * density
                            val t = motion.tilt.value
                            rotationY = t.roll * 4f
                            rotationX = -t.pitch * 4f * 0.7f
                            cameraDistance = 14f * density
                        }
                        .blur(10.dp + 6.dp * motion.lift),
                )
                HoloText(TALK_TITLE, motion.tilt, fontSize = 150.sp, modifier = Modifier.handHeld(motion))
            }
            Spacer(Modifier.height(28.dp))
            Text(TALK_SUBTITLE, color = DeckColors.textPrimary, fontSize = 44.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            Text("DevFest Wrocław 2026", color = DeckColors.textSecondary, fontSize = 34.sp)
        }
        SpeakerBadge(Modifier.align(Alignment.BottomEnd))
    }
}

/** Who I am and what TILT is. */
val aboutMeSlide = Slide {
    Box(Modifier.fillMaxSize().background(StageBackground)) {
        MediaFrame(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(vertical = 60.dp)
                .fillMaxHeight()
                .aspectRatio(720f / 1558f),
        ) {
            VideoClip(resourcePath = "videos/tilt_hero.mp4", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        SpeakerBadge(Modifier.align(Alignment.BottomEnd))
    }
}

/**
 * The spark: Tilt's sign-up coupon. Only the title on screen; the speaker tells the story
 * (see NOTES.md). One click brings in the phone recording of the real onboarding coupon.
 */
val originSlide = Slide(steps = 1) {
    Box(Modifier.fillMaxSize().background(StageBackground)) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 160.dp, vertical = 60.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Tilt onboarding bonus", color = DeckColors.textPrimary, fontSize = 88.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Reveal(at = 1) {
                DeviceRecording("videos/tilt_signup_coupon.mp4", height = 880.dp)
            }
        }
    }
}

/** The goal, said plainly. The next slide shows what everyone else ships. */
val goalSlide = questionSlide(
    "I challenged myself to build\na realistic scratch card",
)

/** Other apps, part 1: no scratching at all. A tap plays a pre-made animation. */
val tapRevealSlide = Slide(steps = 1) {
    LiveSlide(title = "Tap to reveal", subtitle = "how a lot of apps do it") {
        Row(horizontalArrangement = Arrangement.spacedBy(70.dp), verticalAlignment = Alignment.CenterVertically) {
            MediaFrame(Modifier.height(640.dp).aspectRatio(922f / 684f)) {
                VideoClip(resourcePath = "videos/others_tap_reveal.mp4", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            Column(verticalArrangement = Arrangement.spacedBy(30.dp)) {
                Caption("You tap.", color = DeckColors.textPrimary)
                Caption("A pre-made animation plays.", color = DeckColors.textPrimary)
                Caption("Confetti.", color = DeckColors.textPrimary)
                Reveal(at = 1) {
                    Text("No scratching at all.", color = DeckColors.warm, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private data class ScratchifyStage(val shape: BrushShape, val code: String, val caption: String)

private val scratchifyStages = listOf(
    ScratchifyStage(BrushShape.Circle, "BrushShape.Circle", "Scratch it."),
    ScratchifyStage(BrushShape.Star(), "BrushShape.Star()", "Stars!"),
    ScratchifyStage(BrushShape.Heart, "BrushShape.Heart", "Hearts!"),
)

private fun scratchifyCode(shape: String) = """
Scratchify(
    config = ScratchifyConfig(
        revealFullAtPercent = 0.75f,
        brushConfig = ScratchifyBrushConfig(
            brushSize = 18.dp,
            brushColor = Color.Transparent,
            brushShape = $shape,
        ),
    ),
    contentToReveal = { Prize() },
    overlayContent = { Foil() },
)
"""

private val scratchifyFeatures = listOf(
    "circle, square, star, heart, diamond, own Path",
    "auto-reveal at 75%, animated",
    "callbacks, haptics, save & restore",
)

/**
 * Other apps, part 2: Scratchify, a Compose Multiplatform library, running live (vendored copy,
 * see com.gsrathoreniks.scratchify). Each click swaps the brush shape on a fresh card.
 */
val scratchifySlide = Slide(steps = scratchifyStages.lastIndex) {
    val stage = scratchifyStages[step.coerceIn(0, scratchifyStages.lastIndex)]
    BuildSlide(
        title = "Scratchify",
        subtitle = "github.com/gsrathoreniks/Scratchify · Compose Multiplatform",
        code = scratchifyCode(stage.code),
        codeWidth = 900.dp,
        codeFontSize = 24.sp,
        previewSize = 640.dp to 360.dp,
        below = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Caption(stage.caption, color = DeckColors.warm)
                Gap(6.dp)
                scratchifyFeatures.forEach { Caption("· $it") }
            }
        },
    ) {
        key(step) {
            val config = ScratchifyConfig(
                revealFullAtPercent = 0.75f,
                brushConfig = ScratchifyBrushConfig(
                    brushSize = 18.dp,
                    brushColor = Color.Transparent,
                    brushShape = stage.shape,
                ),
            )
            val controller = remember { ScratchifyController(config) }
            Scratchify(
                modifier = Modifier.fillMaxSize(),
                config = config,
                controller = controller,
                contentToReveal = { Box(Modifier.fillMaxSize().background(PrintArea)) { Prize(0.8f) } },
                overlayContent = { Box(Modifier.fillMaxSize().background(FlatFoil)) },
            )
        }
    }
}

private val jsBrushes = listOf("circle", "brush", "spray", "line")

/** Other apps, part 3: the web library with the most brushes. Recording by the speaker. */
val scratchCardJsSlide = Slide {
    LiveSlide(title = "ScratchCard", subtitle = "a JavaScript library with more brushes") {
        Row(horizontalArrangement = Arrangement.spacedBy(70.dp), verticalAlignment = Alignment.CenterVertically) {
            MediaFrame(Modifier.height(600.dp).aspectRatio(16f / 10f)) {
                VideoClip(resourcePath = "videos/others_scratchcard_js.mp4", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                jsBrushes.forEach {
                    Text(it, color = DeckColors.textPrimary, fontSize = 52.sp, fontWeight = FontWeight.SemiBold)
                }
                Gap(10.dp)
                Caption("masth0.github.io/ScratchCard")
            }
        }
    }
}

/**
 * Hook: a recording of a real paper lottery coupon being scratched. The physical thing first;
 * the talk then rebuilds that feeling on a phone. Fit, not crop, so any orientation works.
 */
val hookSlide = Slide {
    Box(Modifier.fillMaxSize().background(StageBackground)) {
        MediaFrame(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 110.dp, vertical = 60.dp)
                .fillMaxSize(),
        ) {
            VideoClip(resourcePath = "videos/real_lottery.mp4", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
    }
}

/** What makes it feel real? Only the question: each chapter answers one part, no spoilers. */
val ingredientsSlide = questionSlide("What makes it feel real?")

@Composable
fun SpeakerBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(end = 110.dp, bottom = 90.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text("Piotr Prus", color = DeckColors.textPrimary, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("@TILT", color = DeckColors.textSecondary, fontSize = 34.sp)
        }
        PhotoImage(
            resourcePath = "images/piotr_avatar.png",
            contentDescription = "Piotr Prus",
            modifier = Modifier.size(260.dp).clip(CircleShape),
        )
    }
}

val introSlides: List<Slide> = listOf(
    titleSlide,
    aboutMeSlide,
    originSlide,
    goalSlide,
    tapRevealSlide,
    scratchifySlide,
    scratchCardJsSlide,
    hookSlide,
    ingredientsSlide,
)
