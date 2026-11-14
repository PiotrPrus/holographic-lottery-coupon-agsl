package dev.piotrprus.holoscratch.presentation.slides

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.piotrprus.holoscratch.presentation.deck.Slide
import dev.piotrprus.holoscratch.presentation.ui.DeckColors
import dev.piotrprus.holoscratch.presentation.ui.PhotoImage
import dev.piotrprus.holoscratch.presentation.demos.TiltArea
import dev.piotrprus.holoscratch.presentation.demos.rememberMouseTilt
import io.github.alexzhirkevich.qrose.rememberQrCodePainter

// "Try it on your phone": one QR code per web demo (the `web/` module on GitHub Pages), shown
// at the end of the chapter that explains it. Plus the Book of Shaders recommendation.

const val WEB_DEMOS_URL = "https://piotrprus.github.io/holographic-lottery-coupon-agsl/"

/** A QR code to one web demo page. [route] is the hash route, e.g. "scratch" → `#/scratch`. */
fun tryItSlide(page: String, route: String, note: String? = null) = Slide {
    val url = "$WEB_DEMOS_URL#/$route"
    LiveSlide(title = "Try it on your phone", subtitle = page) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(90.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QrCode(url, size = 560.dp)
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text(
                    "piotrprus.github.io/\nholographic-lottery-coupon-agsl/\n#/$route",
                    color = DeckColors.textPrimary,
                    fontSize = 34.sp,
                    lineHeight = 48.sp,
                    fontFamily = FontFamily.Monospace,
                )
                if (note != null) Caption(note)
            }
        }
    }
}

/**
 * After "Thank you": the giveaway card (`#/w4b8gx`), 1 in 7 wins one of the goodie bags.
 * The title is printed in holo foil; the emoji sits next to it in plain colour.
 */
val prizesSlide = Slide {
    val tilt = rememberMouseTilt()
    TiltArea(tilt, Modifier.fillMaxSize().background(StageBackground)) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 110.dp, vertical = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(100.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                HoloText("We have", tilt, fontSize = 130.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HoloText("prizes!!", tilt, fontSize = 130.sp)
                    Text("🎉", fontSize = 110.sp, modifier = Modifier.padding(start = 28.dp))
                }
                Gap(36.dp)
                Text("Scratch the card on your phone.\nWin a goodie bag.", color = DeckColors.textPrimary, fontSize = 48.sp, lineHeight = 64.sp)
            }
            QrCode("${WEB_DEMOS_URL}#/w4b8gx", size = 600.dp)
        }
    }
}

/** Last slide: Lumeo, the speaker's own app. Feature graphic, what it does, QR to the site. */
val lumeoSlide = Slide {
    LiveSlide(title = "One more thing: Lumeo", subtitle = "my app · a voice-first second brain") {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(80.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                PhotoImage(
                    resourcePath = "images/lumeo_feature.png",
                    contentDescription = "Lumeo: thoughts in, illumination out",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
                )
                Gap(36.dp)
                Caption("Speak your thoughts. AI turns them into notes,", color = DeckColors.textPrimary)
                Caption("connects them, and finds new ideas.", color = DeckColors.textPrimary)
                Gap(12.dp)
                Caption("Android + iOS · Compose Multiplatform")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                QrCode("https://lumeo-secondbrain.com", size = 460.dp)
                Gap(24.dp)
                Text("lumeo-secondbrain.com", color = DeckColors.textPrimary, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Free book recommendation at the end of the shader chapter. */
val bookOfShadersSlide = Slide {
    LiveSlide(title = "The Book of Shaders", subtitle = "free · by Patricio Gonzalez Vivo and Jen Lowe") {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(70.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhotoImage(
                resourcePath = "images/book_of_shaders.png",
                contentDescription = "The Book of Shaders website",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(1000.dp)
                    .clip(RoundedCornerShape(18.dp)),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                QrCode("https://thebookofshaders.com/", size = 420.dp)
                Gap(24.dp)
                Text("thebookofshaders.com", color = DeckColors.textPrimary, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun QrCode(url: String, size: Dp) {
    Box(
        Modifier
            .size(size)
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(size * 0.07f),
    ) {
        Image(
            painter = rememberQrCodePainter(url),
            contentDescription = "QR code to $url",
            modifier = Modifier.fillMaxSize(),
        )
    }
}
