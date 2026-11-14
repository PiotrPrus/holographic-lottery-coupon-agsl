package dev.piotrprus.holoscratch.web

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val MaxTiltDegrees = 12f

/** The card leans with the phone, like the app's CouponScreen. */
fun Modifier.perspectiveTilt(): Modifier = graphicsLayer {
    val t = Motion.tilt
    rotationY = t.roll * MaxTiltDegrees
    rotationX = -t.pitch * MaxTiltDegrees
    cameraDistance = 14f * density
    transformOrigin = TransformOrigin.Center
}

/**
 * The golden ticket from the app's CouponScreen. [scratchArea] is the 180 dp print area; it
 * gets the prize underneath already, and draws whatever foil it likes on top. [overlay] sits
 * above the print area, outside its clip (where the dust will go).
 */
@Composable
fun Coupon(
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
    scratchArea: @Composable BoxScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(24.dp))
            .background(Paper)
            .padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Label("GOLDEN TICKET", color = Accent, size = 12.sp, weight = FontWeight.Bold, letterSpacing = 2.sp)
            Label("No. 0001 0101 0101", color = Muted, size = 12.sp, mono = true)
        }
        Gap(20)
        Label("SCRATCH & WIN", color = Ink, size = 30.sp, weight = FontWeight.Black, align = TextAlign.Center)
        Gap(6)
        Label("Your chance to win $100 is right here.", color = Muted, size = 14.sp, align = TextAlign.Center)
        Gap(20)
        // Outer box unclipped so dust can spill past the panel edge.
        Box(Modifier.fillMaxWidth().height(180.dp)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .shadow(2.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(PrintArea),
            ) {
                Prize()
                scratchArea()
            }
            overlay()
        }
        Gap(20)
        Canvas(Modifier.fillMaxWidth().height(1.dp)) {
            drawLine(
                color = Muted.copy(alpha = 0.5f),
                start = Offset.Zero,
                end = Offset(size.width, 0f),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
            )
        }
        Gap(12)
        Label(
            "Match 3 symbols to win - No purchase necessary\nValid until 31.12.2026",
            color = Muted, size = 11.sp, align = TextAlign.Center,
        )
    }
}

/** The faint "SCRATCH HERE" printed on the foil itself. */
@Composable
fun ScratchHereHint() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Label("SCRATCH HERE", color = Color.Black.copy(alpha = 0.22f), size = 13.sp, weight = FontWeight.Bold, letterSpacing = 4.sp)
    }
}
