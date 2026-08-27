package com.example.holoscratch.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Shadow
import com.example.holoscratch.holo.HoloFoilPanel
import com.example.holoscratch.holo.ScratchState
import com.example.holoscratch.sensor.Tilt
import com.example.holoscratch.sensor.rememberDeviceTilt

private val Ink = Color(0xFF1B1A2E)
private val Paper = Color(0xFFF7F1E4)
private val Accent = Color(0xFFE0493B)
private val Muted = Color(0xFF7A7690)
private val PrintArea = Color(0xFFEFE7D6)

@Composable
fun CouponScreen(modifier: Modifier = Modifier) {
  val tilt = rememberDeviceTilt()
  CouponScreen(tilt = tilt, modifier = modifier)
}

@Composable
fun CouponScreen(tilt: State<Tilt>, modifier: Modifier = Modifier) {
  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(listOf(Color(0xFF15142A), Color(0xFF2A1F45))))
        .safeDrawingPadding()
        .padding(20.dp),
    contentAlignment = Alignment.Center,
  ) {
    val scratch = remember { ScratchState() }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      CouponCard(
        tilt = { tilt.value },
        scratch = scratch,
        modifier =
          Modifier.graphicsLayer {
            // Perspective tilt: the card leans with the phone. Rolling right edge down
            // rotates around Y so the right edge comes toward the viewer (looks bigger).
            val t = tilt.value
            rotationY = t.roll * MaxTiltDegrees
            rotationX = -t.pitch * MaxTiltDegrees
            cameraDistance = 14f * density
            transformOrigin = TransformOrigin.Center
          },
      )
      Spacer(Modifier.height(24.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        TiltReadout(tilt = tilt)
        Spacer(Modifier.width(20.dp))
        Text(
          text = "reset",
          color = Color.White.copy(alpha = 0.6f),
          fontSize = 13.sp,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.clickable { scratch.reset() }.padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }
    }
  }
}

private const val MaxTiltDegrees = 12f

@Composable
private fun TiltReadout(tilt: State<Tilt>) {
  val t by tilt
  Text(
    text = "roll %+.2f   pitch %+.2f".format(t.roll, t.pitch),
    color = Color.White.copy(alpha = 0.6f),
    fontSize = 13.sp,
    fontFamily = FontFamily.Monospace,
  )
}

@Composable
private fun CouponCard(tilt: () -> Tilt, scratch: ScratchState, modifier: Modifier = Modifier) {
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .shadow(24.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black, spotColor = Color.Black)
        .clip(RoundedCornerShape(24.dp))
        .background(Paper)
        .padding(horizontal = 24.dp, vertical = 28.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(
        "GOLDEN TICKET",
        color = Accent,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
      )
      Text(
        "No. 0001 0101 0101",
        color = Muted,
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
      )
    }

    Spacer(Modifier.height(28.dp))

    Text(
      "SCRATCH & WIN",
      color = Ink,
      fontSize = 34.sp,
      fontWeight = FontWeight.Black,
      letterSpacing = 1.sp,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
      "Your chance to win $100 is right here.",
      color = Muted,
      fontSize = 15.sp,
      textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(28.dp))

    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(180.dp)
          .shadow(2.dp, RoundedCornerShape(14.dp))
          .clip(RoundedCornerShape(14.dp))
          .background(PrintArea)
    ) {
      // Underneath the foil: the prize, printed straight on the ticket.
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("YOU WON", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
          Text("$100", color = Color.Black, fontSize = 56.sp, fontWeight = FontWeight.Black)
        }
      }

      // The aluminium layer on top. Scratching clears foil and sheen together.
      HoloFoilPanel(tilt = tilt, scratch = scratch, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            "SCRATCH HERE",
            color = Color.Black.copy(alpha = 0.22f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp,
          )
        }
      }
    }

    Spacer(Modifier.height(28.dp))

    DashedDivider()

    Spacer(Modifier.height(16.dp))

    Text(
      "Match 3 symbols to win • No purchase necessary\nValid until 31.12.2026",
      color = Muted,
      fontSize = 11.sp,
      lineHeight = 16.sp,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun DashedDivider() {
  Canvas(Modifier.fillMaxWidth().height(1.dp)) {
    drawLine(
      color = Muted.copy(alpha = 0.5f),
      start = Offset.Zero,
      end = Offset(size.width, 0f),
      strokeWidth = 2.dp.toPx(),
      pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
    )
  }
}
