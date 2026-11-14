package dev.piotrprus.holoscratch.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import io.github.kdroidfilter.composemediaplayer.VideoPlayerSurface
import io.github.kdroidfilter.composemediaplayer.rememberVideoPlayerState
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * The native player needs a real file, so each classpath video is extracted
 * once into a temp directory and reused for every slide that shows it.
 */
private object VideoCache {
    private val dir by lazy {
        File(System.getProperty("java.io.tmpdir"), "scratch-to-reveal-videos").apply { mkdirs() }
    }
    private val files = ConcurrentHashMap<String, File>()

    fun fileFor(resourcePath: String): File = files.getOrPut(resourcePath) {
        val target = File(dir, resourcePath.substringAfterLast('/'))
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath)
            ?: error("Missing video resource: $resourcePath")
        stream.use { input -> target.outputStream().use { input.copyTo(it) } }
        target
    }
}

/**
 * Plays a video from classpath resources (e.g. `videos/demo.mp4`) as soon as
 * it enters the composition. Loops and stays muted by default — slides are
 * screen recordings, the speaker does the talking.
 */
@Composable
fun VideoClip(
    resourcePath: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    loop: Boolean = true,
    muted: Boolean = true,
) {
    if (Thread.currentThread().contextClassLoader.getResource(resourcePath) == null) {
        MissingVideo(resourcePath, modifier)
        return
    }
    if (LocalInspectionMode.current) {
        VideoPlaceholder(resourcePath, modifier)
        return
    }
    val player = rememberVideoPlayerState()
    LaunchedEffect(player, resourcePath) {
        player.loop = loop
        player.volume = if (muted) 0f else 1f
        player.openUri(VideoCache.fileFor(resourcePath).toURI().toString())
    }
    VideoPlayerSurface(
        playerState = player,
        modifier = modifier,
        contentScale = contentScale,
    )
}

/** Stand-in for offscreen renders and previews, where the native player can't run. */
@Composable
private fun VideoPlaceholder(resourcePath: String, modifier: Modifier) {
    Box(
        modifier = modifier.background(Color(0xFF3A1F4D)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "▶ " + resourcePath.substringAfterLast('/'),
            color = Color.White,
            fontSize = 28.sp,
        )
    }
}

/**
 * Slot for a recording that has not been added yet: tells the speaker where to drop the file.
 * Drop the clip at `presentation/src/main/resources/<resourcePath>` and it plays here.
 */
@Composable
private fun MissingVideo(resourcePath: String, modifier: Modifier) {
    Box(
        modifier = modifier.background(Color(0xFF1B2030)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Device recording\n\nresources/$resourcePath",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 22.sp,
            lineHeight = 30.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
