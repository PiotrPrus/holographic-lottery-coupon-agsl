package dev.piotrprus.holoscratch.web

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.sp

/** 3. The holo foil shader alone, big, leaning with the phone. */
@Composable
fun FoilPage() {
    Page("Holo foil") {
        Label("Tilt the phone. Light, rainbow and glitter follow roll and pitch.", color = Dim, size = 14.sp)
        Gap(24)
        HoloFoilPanel(
            tilt = { Motion.tilt },
            modifier = Modifier
                .perspectiveTilt()
                .fillMaxWidth()
                .aspectRatio(0.8f)
                .shadow(18.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp)),
        )
        Gap(20)
        TiltReadout()
        Gap(10)
        MotionStatus()
    }
}

/** 4. Holo foil, scratched with the coin blade. */
@Composable
fun CoinScratchPage() {
    val scratch = remember { ScratchState() }
    Page("Coin scratch") {
        Label("A coin edge, not a round pen: a thin blade turned across the stroke, wobbling every 9 px.", color = Dim, size = 14.sp)
        Gap(16)
        Coupon(Modifier.perspectiveTilt()) {
            HoloFoilPanel(tilt = { Motion.tilt }, scratch = scratch, modifier = Modifier.fillMaxSize()) { ScratchHereHint() }
        }
        Gap(18)
        PillButton("Reset", onClick = { scratch.reset() })
        Gap(10)
        MotionStatus()
    }
}

/** 5. Everything: coin scratch, holo foil, dust, sound grains, vibration. */
@Composable
fun FinalPage() {
    val scratch = remember { ScratchState() }
    ScratchSound(scratch)
    Page("The whole card") {
        Label("Sound on. Scratch fresh foil to hear it and, on Android, feel it.", color = Dim, size = 14.sp)
        Gap(16)
        Coupon(
            Modifier.perspectiveTilt(),
            overlay = {
                // Dust rides above the foil and outside its clip, as in the app.
                ScratchDust(scratch = scratch, modifier = Modifier.matchParentSize())
            },
        ) {
            HoloFoilPanel(tilt = { Motion.tilt }, scratch = scratch, modifier = Modifier.fillMaxSize()) { ScratchHereHint() }
        }
        Gap(18)
        PillButton("Reset", onClick = { scratch.reset() })
        Gap(10)
        MotionStatus()
        // holo.js unlocks Web Audio on the first tap; poll until the grains are decoded.
        val soundOn by produceState(audioReady()) {
            while (!value) {
                delay(250)
                value = audioReady()
            }
        }
        if (!soundOn) {
            Gap(6)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Label(
                    "Tap once to switch the sound on. iPhone: the ring/silent switch must be on ring.",
                    color = Color.White.copy(alpha = 0.45f), size = 11.sp, align = TextAlign.Center,
                )
            }
        }
    }
}
