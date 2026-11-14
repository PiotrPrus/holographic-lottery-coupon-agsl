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
 * One page per demo, each with its own URL so a QR code can point straight at it.
 * Hash routes (`#/coin`) because GitHub Pages serves static files only: a real subpath would
 * 404 on reload, a hash never reaches the server.
 */
enum class Route(val hash: String, val title: String, val blurb: String) {
    Index("", "Scratch to Reveal", ""),
    Scratch("scratch", "Simple scratch", "Grey paint, round thumb-sized pen, BlendMode.Clear."),
    Sensor("sensor", "Gravity sensor check", "Live x, y, z from your phone, roll and pitch, and an arrow that points down."),
    Foil("foil", "Foil", "The holographic foil shader, driven by your phone's tilt."),
    Coin("coin", "Coin scratch", "Holo foil scratched with a coin edge instead of a round pen."),
    Final("final", "Final", "Coin scratch + holo foil + sound + vibration."),
    ;

    companion object {
        fun fromHash(hash: String): Route {
            val key = hash.removePrefix("#").removePrefix("/").substringBefore('?').trimEnd('/')
            return entries.firstOrNull { it != Index && it.hash == key } ?: Index
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
    // Browser back/forward and hand-edited URLs change the hash behind our back.
    LaunchedEffect(Unit) {
        while (true) {
            delay(150)
            val now = Route.fromHash(locationHash())
            if (now != route) route = now
        }
    }
    val go: (Route) -> Unit = { r ->
        setLocationHash(if (r == Route.Index) "#/" else "#/${r.hash}")
        route = r
    }
    val back = { go(Route.Index) }

    MotionLoop()
    Box(Modifier.fillMaxSize().pointerTiltFallback()) {
        when (route) {
            Route.Index -> IndexPage(go)
            Route.Scratch -> SimpleScratchPage(back)
            Route.Sensor -> SensorPage(back)
            Route.Foil -> FoilPage(back)
            Route.Coin -> CoinScratchPage(back)
            Route.Final -> FinalPage(back)
        }
    }
}

@Composable
private fun IndexPage(go: (Route) -> Unit) {
    Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
        ) {
            Label("Scratch to Reveal", size = 28.sp, weight = FontWeight.Black)
            Gap(6)
            Label("A holographic scratch card, built step by step. Open the demos on your phone.", color = Dim, size = 14.sp)
            Gap(20)
            Route.entries.filter { it != Route.Index }.forEachIndexed { i, r ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .clickable { go(r) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Label("${i + 1}. ${r.title}", size = 18.sp, weight = FontWeight.Bold)
                    Gap(3)
                    Label(r.blurb, color = Dim, size = 13.sp)
                }
            }
            Gap(16)
            Label(
                "Needs a recent browser (Chrome, Firefox, Safari on iOS 18.2+). On iPhone, tap \"Enable motion\" when asked.",
                color = Color.White.copy(alpha = 0.45f), size = 11.sp,
            )
        }
    }
}
