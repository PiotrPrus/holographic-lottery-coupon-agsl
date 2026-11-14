package dev.piotrprus.holoscratch.presentation.slides

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.piotrprus.holoscratch.presentation.deck.Slide
import dev.piotrprus.holoscratch.presentation.demos.HoloCoupon
import dev.piotrprus.holoscratch.presentation.demos.ScratchConfig
import dev.piotrprus.holoscratch.presentation.demos.ScratchState
import dev.piotrprus.holoscratch.presentation.demos.TiltArea
import dev.piotrprus.holoscratch.presentation.demos.TiltReadout
import dev.piotrprus.holoscratch.presentation.demos.rememberMouseTilt
import dev.piotrprus.holoscratch.presentation.ui.DeckColors
import io.github.alexzhirkevich.qrose.rememberQrCodePainter

const val REPO_URL = "https://github.com/PiotrPrus/holographic-lottery-coupon-agsl"

/** Everything together, live, next to the phone recording. */
val finaleSlide = Slide {
    val tilt = rememberMouseTilt()
    val scratch = remember { ScratchState(ScratchConfig.Coin) }
    TiltArea(tilt, Modifier.fillMaxSize().background(StageBackground)) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 110.dp, vertical = 60.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HoloCoupon(tilt, scratch)
                Spacer(Modifier.height(36.dp))
                TiltReadout(tilt)
            }
        }
    }
}

private val recap = listOf(
    "It scratches" to "graphicsLayer Offscreen + BlendMode.Clear",
    "It knows how you hold it" to "TYPE_GRAVITY + a little smoothing",
    "It leans" to "graphicsLayer rotationX / rotationY",
    "It shines" to "AGSL + RuntimeShader + uniforms",
    "It leaves crumbs" to "CanvasParticleEmitter + graph paper",
    "It scrapes like a coin" to "a filled Path that turns and wobbles",
    "It sounds like foil" to "short grains, one per bit of finger travel",
)

/** What we used, in the words of the talk. */
val recapSlide = Slide(steps = recap.size - 1) {
    val tilt = rememberMouseTilt()
    TiltArea(tilt, Modifier.fillMaxSize().background(StageBackground)) {
        Column(Modifier.align(Alignment.CenterStart).padding(start = 160.dp), verticalArrangement = Arrangement.spacedBy(30.dp)) {
            recap.forEachIndexed { index, (what, how) ->
                Reveal(at = index) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        HoloText(what, tilt, fontSize = 56.sp, modifier = Modifier.width(820.dp))
                        Text(how, color = DeckColors.accentAlt, fontSize = 32.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 10.dp))
                    }
                }
            }
        }
    }
}

/** Closing slide: QR to the repo, speaker badge. */
val theEndSlide = Slide {
    val tilt = rememberMouseTilt()
    TiltArea(tilt, Modifier.fillMaxSize().background(StageBackground)) {
        Column(
            modifier = Modifier.align(Alignment.Center).padding(bottom = 140.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HoloText("Thank you", tilt, fontSize = 170.sp)
            Spacer(Modifier.height(24.dp))
            Text(TALK_TITLE, color = DeckColors.textSecondary, fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 110.dp, bottom = 90.dp)) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .background(Color.White, RoundedCornerShape(18.dp))
                    .padding(22.dp),
            ) {
                Image(
                    painter = rememberQrCodePainter(REPO_URL),
                    contentDescription = "QR code to the demo repository",
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("code + slides", color = DeckColors.textSecondary, fontSize = 26.sp)
        }
        SpeakerBadge(Modifier.align(Alignment.BottomEnd))
    }
}

val outroSlides: List<Slide> = listOf(finaleSlide, tryItSlide("The whole card", "q5r1fy", note = "Sound on. Android also vibrates."), recapSlide, theEndSlide, prizesSlide)
