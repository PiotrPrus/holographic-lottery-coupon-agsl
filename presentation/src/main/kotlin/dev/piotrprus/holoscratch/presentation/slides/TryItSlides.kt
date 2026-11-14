package dev.piotrprus.holoscratch.presentation.slides

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
