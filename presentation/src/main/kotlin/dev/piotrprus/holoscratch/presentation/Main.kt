package dev.piotrprus.holoscratch.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.piotrprus.holoscratch.presentation.deck.DeckState
import dev.piotrprus.holoscratch.presentation.deck.SlideScope
import dev.piotrprus.holoscratch.presentation.slides.deckSlides
import dev.piotrprus.holoscratch.presentation.ui.DeckColors
import dev.piotrprus.holoscratch.presentation.ui.PresentationTheme

/** Slides are designed against this fixed logical canvas and scaled to fit. */
private val STAGE_WIDTH = 1920.dp
private val STAGE_HEIGHT = 1080.dp

/**
 * Launch options:
 *  - `--slide=N` starts on slide N (1-based, same number as the on-screen counter).
 *  - `--windowed` opens in a normal window instead of fullscreen.
 */
fun main(args: Array<String>) = application {
    val startSlide = args.firstNotNullOfOrNull { it.removePrefix("--slide=").takeIf { v -> v != it } }
        ?.toIntOrNull() ?: 1
    val windowed = "--windowed" in args
    val windowState = rememberWindowState(
        placement = if (windowed) WindowPlacement.Floating else WindowPlacement.Fullscreen,
        size = DpSize(1280.dp, 720.dp),
    )
    val deck = remember { DeckState(deckSlides(), startIndex = startSlide - 1) }

    Window(
        onCloseRequest = ::exitApplication,
        alwaysOnTop = !windowed,
        state = windowState,
        title = "Scratch to Reveal",
        onKeyEvent = { event ->
            if (event.type != KeyEventType.KeyDown) return@Window false
            when (event.key) {
                Key.DirectionRight, Key.Spacebar, Key.Enter, Key.DirectionDown -> {
                    deck.next(); true
                }
                Key.DirectionLeft, Key.DirectionUp -> {
                    deck.previous(); true
                }
                Key.Escape -> {
                    windowState.placement = WindowPlacement.Floating; true
                }
                Key.F -> {
                    windowState.placement = WindowPlacement.Fullscreen; true
                }
                else -> false
            }
        },
    ) {
        PresentationTheme {
            Stage {
                AnimatedContent(
                    targetState = deck.slideIndex,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(tween(500)) { it } + fadeIn(tween(500)))
                                .togetherWith(
                                    slideOutHorizontally(tween(500)) { -it } + fadeOut(tween(500)),
                                )
                        } else {
                            (slideInHorizontally(tween(500)) { -it } + fadeIn(tween(500)))
                                .togetherWith(
                                    slideOutHorizontally(tween(500)) { it } + fadeOut(tween(500)),
                                )
                        }
                    },
                ) { index ->
                    val slide = deck.slides[index]
                    // The outgoing slide and back-navigation both render fully revealed.
                    val step = if (index == deck.slideIndex) deck.step else slide.steps
                    Box(modifier = Modifier.fillMaxSize()) {
                        slide.content(SlideScope(step))
                    }
                }
                Text(
                    text = "${deck.slideIndex + 1} / ${deck.slides.size}",
                    color = DeckColors.textSecondary.copy(alpha = 0.6f),
                    fontSize = 22.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(36.dp),
                )
            }
        }
    }
}

/**
 * Keynote-style stage: a fixed 1920×1080 logical canvas, uniformly scaled to
 * fit the actual window and letterboxed on black.
 */
@Composable
private fun Stage(content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        val scale = minOf(maxWidth / STAGE_WIDTH, maxHeight / STAGE_HEIGHT)
        Box(
            modifier = Modifier
                .requiredSize(STAGE_WIDTH, STAGE_HEIGHT)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .background(DeckColors.background),
            content = content,
        )
    }
}
