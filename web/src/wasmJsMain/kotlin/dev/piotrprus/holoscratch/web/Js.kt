@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.piotrprus.holoscratch.web

// Thin bridge to holo.js. In Kotlin/Wasm a js("…") call must be the whole body of a top-level
// function; parameters are visible to the snippet by name.

internal fun motionX(): Double = js("window.holoMotion.x")
internal fun motionY(): Double = js("window.holoMotion.y")
internal fun motionZ(): Double = js("window.holoMotion.z")
internal fun motionRawX(): Double = js("window.holoMotion.rawX")
internal fun motionRawY(): Double = js("window.holoMotion.rawY")
internal fun motionRawZ(): Double = js("window.holoMotion.rawZ")
internal fun motionSign(): Int = js("window.holoMotion.sign")
internal fun motionSignSource(): String = js("window.holoMotion.signSource")
internal fun motionEvents(): Int = js("window.holoMotion.events")
internal fun motionState(): String = js("window.holoMotion.state")

internal fun audioWant(): Unit = js("window.holoAudio.want()")
internal fun audioReady(): Boolean = js("window.holoAudio.isRunning()")
internal fun audioPlay(volume: Double, rate: Double): Unit = js("window.holoAudio.play(volume, rate)")

internal fun vibrate(ms: Int): Unit = js("window.holoVibrate(ms)")
internal fun canVibrate(): Boolean = js("typeof navigator.vibrate === 'function'")

internal fun locationHash(): String = js("window.location.hash")

/** True only when served from this computer, never on GitHub Pages. */
internal fun isLocalhost(): Boolean = js("['localhost', '127.0.0.1'].includes(window.location.hostname)")
internal fun setLocationHash(hash: String): Unit = js("window.location.hash = hash")

internal fun nowMs(): Double = js("performance.now()")
internal fun booted(): Unit = js("window.holoBooted()")
