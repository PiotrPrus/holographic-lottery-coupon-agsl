package dev.piotrprus.holoscratch.web

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeViewport
import kotlinx.coroutines.delay

/**
 * One page per demo, each opened only by its own QR code in the talk.
 * Hash routes (`#/k7p2qx`) because GitHub Pages serves static files only: a real subpath would
 * 404 on reload, a hash never reaches the server. The keys are opaque on purpose, so nobody can
 * guess `#/final` and jump ahead; any unknown hash shows the landing page.
 */
enum class Route(val hash: String) {
    Landing(""),
    Scratch("k7p2qx"),
    Sensor("m3v8ra"),
    Foil("t9c4wd"),
    Coin("h2n6zb"),
    Final("q5r1fy"),
    ;

    companion object {
        fun fromHash(hash: String): Route {
            val key = hash.removePrefix("#").removePrefix("/").substringBefore('?').trimEnd('/')
            return entries.firstOrNull { it != Landing && it.hash == key } ?: Landing
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport { App() }
    booted()
}

@Composable
fun App() {
    var route by remember { mutableStateOf(Route.fromHash(locationHash())) }
    // A new QR scan in the same tab, or browser back/forward, changes only the hash.
    LaunchedEffect(Unit) {
        while (true) {
            delay(150)
            val now = Route.fromHash(locationHash())
            if (now != route) route = now
        }
    }

    MotionLoop()
    Box(Modifier.fillMaxSize().pointerTiltFallback()) {
        when (route) {
            Route.Landing -> LandingPage()
            Route.Scratch -> SimpleScratchPage()
            Route.Sensor -> SensorPage()
            Route.Foil -> FoilPage()
            Route.Coin -> CoinScratchPage()
            Route.Final -> FinalPage()
        }
    }
}

/** What the bare site shows: no list of demos, just a pointer back to the talk. */
@Composable
private fun LandingPage() {
    Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 520.dp).fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Label("Scratch to Reveal", size = 28.sp, weight = FontWeight.Black)
            Gap(10)
            Label("Scan the QR code on the slide to open the next demo.", color = Dim, size = 15.sp)
            Gap(16)
            Label(
                "Needs a recent browser (Chrome, Firefox, Safari on iOS 18.2+).",
                color = Color.White.copy(alpha = 0.45f), size = 11.sp,
            )
        }
    }
}
