package dev.piotrprus.holoscratch.presentation.slides

import dev.piotrprus.holoscratch.presentation.deck.Slide

/** Deck sections in talk order; keys are used by `./gradlew :presentation:renderSlides -Psection=…`. */
fun deckSections(): Map<String, List<Slide>> = linkedMapOf(
    "intro" to introSlides,
    "scratch" to scratchSlides,
    "shine" to shineSlides,
    "tilt" to tiltSlides,
    "crumbs" to crumbsSlides,
    "coin" to coinSlides,
    "sound" to soundSlides,
    "outro" to outroSlides,
)

fun deckSlides(): List<Slide> = deckSections().values.flatten()
