package dev.piotrprus.holoscratch.presentation.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Density
import dev.piotrprus.holoscratch.presentation.deck.SlideScope
import dev.piotrprus.holoscratch.presentation.slides.deckSections
import dev.piotrprus.holoscratch.presentation.slides.deckSlides
import dev.piotrprus.holoscratch.presentation.ui.DeckColors
import dev.piotrprus.holoscratch.presentation.ui.PresentationTheme
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Dev tool: renders every slide at every reveal step into PNGs without opening
 * a window. Videos show as placeholders (LocalInspectionMode).
 *
 * `./gradlew renderSlides [-Pout=build/slides] [-Psection=animation] [-Pslides=1,2]`
 *
 * With a section, slide numbers are local to that section and files are
 * prefixed with its name; otherwise they are global deck positions.
 */
fun main(args: Array<String>) {
    val out = File(args.getOrElse(0) { "build/slides" }).apply { mkdirs() }
    val only = args.getOrNull(1)?.split(',')?.filter { it.isNotBlank() }?.map { it.trim().toInt() }?.toSet()?.takeIf { it.isNotEmpty() }
    val section = args.getOrNull(2)?.takeIf { it.isNotBlank() }
    val slides = if (section != null) {
        deckSections()[section] ?: error("Unknown section '$section', known: ${deckSections().keys}")
    } else {
        deckSlides()
    }
    val prefix = section?.let { "${it}_" } ?: ""
    slides.forEachIndexed { index, slide ->
        val number = index + 1
        if (only != null && number !in only) return@forEachIndexed
        for (step in 0..slide.steps) {
            val scene = ImageComposeScene(width = 1920, height = 1080, density = Density(1f)) {
                CompositionLocalProvider(LocalInspectionMode provides true) {
                    PresentationTheme {
                        Box(modifier = Modifier.fillMaxSize().background(DeckColors.background)) {
                            slide.content(SlideScope(step))
                        }
                    }
                }
            }
            // A few intermediate frames so effects that wait for a frame (layout sizes, first frames) settle.
            scene.render(0)
            scene.render(500_000_000L)
            scene.render(1_000_000_000L)
            scene.render(2_000_000_000L)
            val image = scene.render(3_000_000_000L)
            val data = image.encodeToData(EncodedImageFormat.PNG) ?: error("Encoding failed")
            File(out, "%sslide%03d_step%d.png".format(prefix, number, step)).writeBytes(data.bytes)
            scene.close()
        }
    }
    println("Rendered to ${out.absolutePath}")
}
