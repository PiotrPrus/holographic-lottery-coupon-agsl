package dev.piotrprus.holoscratch.presentation.demos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.util.concurrent.Executors
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import kotlin.math.log10
import kotlin.math.max
import kotlin.random.Random

/** Finger travel on fresh foil between two scrape sounds. Faster scratching = denser sound. */
private const val GrainStepPx = 36f

/** Below this dust strength the finger is on bare paper: no sound. */
private const val MinLevel = 0.05f

/**
 * Desktop copy of the app's `holo/ScratchSound.kt`.
 *
 * The recording is cut into short grains (~120 ms each, `resources/sounds/scratch_grain_NN.wav`).
 * One grain plays every [GrainStepPx] of finger travel while dust comes out, so the sound is tied
 * to the movement: a quick swipe gives a burst of scrapes, a slow drag a few separate ones, and
 * the very first fresh move plays one immediately. [level] is the dust strength (0..1) and sets
 * the volume.
 *
 * Side-by-side slides scratch two panels with one finger. Grains are rate-limited globally, so
 * the mirrored panel doesn't double every scrape.
 */
@Composable
fun ScratchSound(level: Float, position: Offset?) {
    if (LocalInspectionMode.current) return
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { ScratchGrains.preload() } }
    val tracker = remember { GrainTracker() }
    SideEffect { tracker.update(level, position) }
}

private class GrainTracker {
    private var last: Offset? = null

    // Starts full, so the first move over fresh foil sounds at once.
    private var travel = GrainStepPx

    fun update(level: Float, position: Offset?) {
        if (position == null) {
            last = null
            travel = GrainStepPx
            return
        }
        val previous = last
        last = position
        if (previous != null) travel += (position - previous).getDistance()
        if (level < MinLevel || travel < GrainStepPx) return
        travel = 0f
        ScratchGrains.play(level)
    }
}

internal object ScratchGrains {
    private const val Count = 16
    private const val MinIntervalMs = 45L
    private const val MinDb = -40f

    // Clip.start/stop can block for a moment; keep that off the UI thread.
    private val audio = Executors.newSingleThreadExecutor { Thread(it, "scratch-sound").apply { isDaemon = true } }

    @Volatile
    private var clips: List<Clip>? = null
    private var lastIndex = -1
    private var lastPlayMs = 0L

    /** Opens every grain up front: opening a clip on first use is where the start-up delay hides. */
    @Synchronized
    fun preload() {
        if (clips != null) return
        clips = (0 until Count).mapNotNull { i ->
            runCatching {
                val name = "sounds/scratch_grain_%02d.wav".format(i)
                val stream = ScratchGrains::class.java.classLoader.getResourceAsStream(name) ?: return@runCatching null
                AudioSystem.getAudioInputStream(BufferedInputStream(stream)).use { input ->
                    AudioSystem.getClip().apply { open(input) }
                }
            }.getOrNull()
        }
    }

    /** One grain at full volume, ignoring the rate limit: for the play button on the slide. */
    fun playNow() {
        lastPlayMs = 0L
        play(1f)
    }

    fun play(level: Float) {
        val now = System.currentTimeMillis()
        synchronized(this) {
            if (now - lastPlayMs < MinIntervalMs) return
            lastPlayMs = now
        }
        audio.execute {
            val all = clips ?: run { preload(); clips } ?: return@execute
            if (all.isEmpty()) return@execute
            var index = Random.nextInt(all.size)
            if (index == lastIndex && all.size > 1) index = (index + 1) % all.size
            lastIndex = index
            val clip = all[index]
            clip.stop()
            clip.framePosition = 0
            (clip.getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl)?.let { gain ->
                val volume = 0.4f + 0.6f * level.coerceIn(0f, 1f)
                gain.value = (20f * log10(volume)).coerceIn(max(gain.minimum, MinDb), gain.maximum.coerceAtMost(0f))
            }
            clip.start()
        }
    }
}

/** Buttons on the sound slides: play the untouched recording, or one grain, on demand. */
object SoundBench {
    private val original: Clip? by lazy { openClip("sounds/scratch_original.wav") }

    fun playOriginal() {
        val clip = original ?: return
        clip.stop()
        clip.framePosition = 0
        clip.start()
    }

    fun playGrain() = ScratchGrains.playNow()

    /** Peak amplitude (0..1) per bucket of the original recording, for the waveform pictures. */
    val originalPeaks: Pair<FloatArray, Float> by lazy { loadPeaks("sounds/scratch_original.wav", buckets = 1200) }
}

private fun openClip(resource: String): Clip? = runCatching {
    val stream = SoundBench::class.java.classLoader.getResourceAsStream(resource) ?: return@runCatching null
    AudioSystem.getAudioInputStream(BufferedInputStream(stream)).use { input ->
        AudioSystem.getClip().apply { open(input) }
    }
}.getOrNull()

/** Returns peaks per bucket and the total duration in seconds. Expects 16-bit mono PCM. */
private fun loadPeaks(resource: String, buckets: Int): Pair<FloatArray, Float> {
    val stream = SoundBench::class.java.classLoader.getResourceAsStream(resource) ?: return FloatArray(0) to 0f
    AudioSystem.getAudioInputStream(BufferedInputStream(stream)).use { input ->
        val bytes = input.readAllBytes()
        val samples = bytes.size / 2
        val seconds = samples / input.format.sampleRate
        val bigEndian = input.format.isBigEndian
        val peaks = FloatArray(buckets)
        val per = (samples / buckets).coerceAtLeast(1)
        for (b in 0 until buckets) {
            var peak = 0
            for (i in b * per until minOf((b + 1) * per, samples)) {
                val lo = bytes[i * 2].toInt() and 0xFF
                val hi = bytes[i * 2 + 1].toInt()
                val v = if (bigEndian) (lo shl 8 or (hi and 0xFF)).toShort().toInt() else (hi shl 8 or lo).toShort().toInt()
                peak = max(peak, kotlin.math.abs(v))
            }
            peaks[b] = peak / 32768f
        }
        return peaks to seconds
    }
}
