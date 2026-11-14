package dev.piotrprus.holoscratch.presentation.demos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF1B1A2E)
private val Paper = Color(0xFFF7F1E4)
private val Accent = Color(0xFFE0493B)
private val Muted = Color(0xFF7A7690)

const val MAX_TILT_DEGREES = 12f
const val CAMERA_DISTANCE = 14f

/** The perspective lean from CouponScreen: the card follows the phone like a card on a table. */
fun Modifier.lean(
    tilt: () -> Tilt,
    maxDegrees: Float = MAX_TILT_DEGREES,
    cameraDistance: Float = CAMERA_DISTANCE,
): Modifier = graphicsLayer {
    val t = tilt()
    rotationY = t.roll * maxDegrees
    rotationX = -t.pitch * maxDegrees
    this.cameraDistance = cameraDistance * density
    transformOrigin = TransformOrigin.Center
}

/** The full golden ticket from the Android app, sized for the 1920×1080 stage. */
@Composable
fun HoloCoupon(
    tilt: MouseTilt,
    scratch: ScratchState,
    modifier: Modifier = Modifier,
    leanEnabled: Boolean = true,
    dust: DustGate = DustGate.Swept,
    sound: Boolean = true,
) {
    Column(
        modifier = modifier
            .width(680.dp)
            .then(if (leanEnabled) Modifier.lean({ tilt.value }) else Modifier)
            .shadow(36.dp, RoundedCornerShape(36.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(36.dp))
            .background(Paper)
            .padding(horizontal = 40.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("GOLDEN TICKET", color = Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Text("No. 0001 0101 0101", color = Muted, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(40.dp))
        Text(
            "SCRATCH & WIN",
            color = Ink,
            fontSize = 54.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text("Your chance to win $100 is right here.", color = Muted, fontSize = 22.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(40.dp))
        ScratchPanel(
            scratch = scratch,
            tilt = { tilt.value },
            dust = dust,
            sound = sound,
            modifier = Modifier.fillMaxWidth().height(270.dp),
            prizeScale = 0.8f,
            bladeLength = 38.dp,
            bladeThickness = 6.dp,
        )
        Spacer(Modifier.height(40.dp))
        Canvas(Modifier.fillMaxWidth().height(1.dp)) {
            drawLine(
                color = Muted.copy(alpha = 0.5f),
                start = Offset.Zero,
                end = Offset(size.width, 0f),
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 14f)),
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            "Match 3 symbols to win • No purchase necessary\nValid until 31.12.2026",
            color = Muted,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/** Small monospace roll/pitch readout, like the one under the card in the app. */
@Composable
fun TiltReadout(tilt: MouseTilt, modifier: Modifier = Modifier) {
    val t = tilt.value
    Box(modifier) {
        Text(
            text = "roll %+.2f   pitch %+.2f".format(t.roll, t.pitch),
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 24.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}
