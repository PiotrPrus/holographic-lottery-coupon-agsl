# Speaker notes: Scratch to Reveal (DevFest Wrocław 2026)

**Format:** about 30 minutes, 44 slides, five chapters. Each chapter follows the same rhythm: show the effect live, then build it step by step. On the build slides the snippet grows on the left, and the preview runs exactly that code.

**Controls**

| Key | Action |
|---|---|
| `→` / `Space` / `Enter` | Next reveal or slide |
| `←` | Previous slide (fully revealed) |
| `Esc` | Leave fullscreen |
| `F` | Back to fullscreen |

**Live input**
- Scratching plays short foil-scrape sounds (`resources/sounds/scratch_grain_NN.wav`), one per bit of finger travel, only while dust comes out. Fast swipe = burst; bare paper = silent. Turn the laptop audio on.
- Move the mouse anywhere to tilt the foil. If the mouse stays still for about 2 s, the card wobbles on its own.
- Drag on any panel to scratch it. Nothing ever scratches by itself.
- On side-by-side slides, one drag scratches both panels.

**Commands**
- Run the deck: `./gradlew :presentation:run`
- Start on slide 8: `./gradlew :presentation:run -Pslide=8`
- Open in a window instead of fullscreen: `./gradlew :presentation:run -Pwindowed` (combine with `-Pslide=8`)
- Render every step to PNG: `./gradlew :presentation:renderSlides`. Add `-Psection=crumbs` to render one section.

## Device recordings to add

Drop the files into `presentation/src/main/resources/videos/`. Until a file is there, its slot shows the path it expects.

| File | Slide | What to record |
|---|---|---|
| `tilt_signup_coupon.mp4` | It started with a coupon | Added (Tilt onboarding scratch) |
| `others_tap_reveal.mp4` | Tap to reveal | Added (LottieFiles recording) |
| `others_scratchcard_js.mp4` | ScratchCard (JS) | Added (brushes demo recording) |
| `real_foil_reflections.mp4` | How do you hold it? | Added (real coupon rolled and pitched under a light, portrait, 13 s) |
| `real_lottery.mp4` | Hook | Added (real coupon scratched, landscape, 30 s) |
| `device_tilt.mp4` | Gravity sensor | Added (the app's Gravity sensor screen on the Pixel, in a hand, muted) |
| `real_sticker.mp4` | How do you hold it? | Added (DevFest Wrocław holo sticker in a hand, portrait) |

Use portrait (9:19.5), muted, short loops of 5–15 s.

## Timing

| Section | Target |
|---|---|
| Intro: title → "What makes it feel real?" | 3:30 |
| Let's scratch! | 4:00 |
| Make it shine | 7:00 |
| Make it react to your hand | 5:00 |
| Every scratch leaves crumbs | 6:00 |
| Nobody scratches with a marker | 3:30 |
| Finale, recap, thanks | 1:30 |

---

## Intro
- **Title.** The title is printed in the foil shader. It rolls, pitches and lifts on its own, so the foil shimmers while you talk.
- **About me.**
- **Tilt onboarding bonus.** Only the title on screen; tell the story yourself. Click: the phone recording.
  - Sign up on Tilt and you get a welcome bonus.
  - How much? Nobody knows. It's random.
  - Scratch the coupon to find out.
  - Krzysztof built it on Android.
- **"I challenged myself to build a realistic scratch card."** Say it plainly.
- **Tap to reveal.** The LottieFiles recording: you tap, a pre-made animation plays, confetti. Click: "No scratching at all."
- **Scratchify.** A Compose Multiplatform library, running live (vendored copy, MIT). Scratch it. Click: star brush. Click: heart brush. Mention what it offers: brush shapes, auto-reveal at 75%, callbacks, haptics, save and restore. Then say you wanted something more.
- **ScratchCard (JS).** The web library with four brushes: circle, brush, spray, line.
- **Hook.** A recording of a real paper lottery coupon being scratched. The physical thing first.
- **"What makes it feel real?"** Only the question, no list. Let the room guess; each chapter answers one part.

## Let's scratch!
- **Build slide.** Scratch the preview at every step. It goes from buffer to blend mode.
  - Box (just the prize).
  - Grey layer: just grey paint on top of the prize. It cannot feel the finger yet.
  - pointerInput: we drew a line. We drew on the foil; we didn't scratch it.
  - CompositingStrategy.Offscreen: the foil now draws into its own buffer. Looks the same, for now.
  - The KDoc box lands on top of the code: an offscreen buffer, a separate picture of just the foil.
  - BlendMode: source (the path we draw now) + destination (what is already there: the foil's buffer) = result. The code says `SrcOver // default`, and the diagram shows only SrcOver: the path paints on top.
  - Same line becomes `BlendMode.Clear`. The diagram adds Clear next to SrcOver; the KDoc: "Drop both the source and destination images, leaving nothing."
  - BlendMode.Clear live: there it is. 🎉 Clear erases inside the foil's buffer only, so the prize shows through.
- **change.historical.** The screen samples faster than frames, so one event per frame turns a fast curve into straight chords. Click: the KDoc and the in-between points. Click: replay them oldest first, then change.position. Works inside detectDragGestures' onDrag. Click: two live panels, one stroke drives both. Scratch fast circles: the top one (only change.position) gets corners, the bottom one stays round. Reset. A desktop mouse never batches points, so the top panel keeps one point per 16 ms frame to show what a phone without historical does.

## Make it shine
- **"What is a shader?"** Click for the answer.
- **First shader.** Pink box → gradient (uv) → uTilt, a uniform sent every frame: two numbers from −1 to 1, roll and pitch, made by the mouse for now → light: tilt 0 puts it in the middle, roll moves it left/right, pitch up/down (the cross marks it) → glow: 1 under the lamp, fading with distance; 6.0 = how tight; 0.35 = the dark grey.
- **RuntimeShader.** The shader is a String → set the size once in the cache → set the tilt every frame.
- **Foil build.** Grey → grain → lamp → rainbow → glitter → glare. A few lines per layer. Move the mouse on each step.
- **Rainbow speed.** One moving piece at a time.
  - Only `rainbow(phase)`, no foil: phase is a position on the colour wheel, 0 = red, 0.5 = cyan, 1.0 = red again (the ruler). `p.y * 1.8`: top to bottom of the card = 1.8 rainbows, so you see stripes.
  - The mouse adds `roll * 0.6`: edge to edge adds 1.2, the stripes slide by about one rainbow.
  - The same colours on the foil, under the stripes: `band` keeps them only near the light.
  - My first try, 2.0 and 3.0: the stripes race, an oil slick on the foil.
  - Both foils side by side: light moves fast, colour crawls.
- **Where does the shine live?** Problem first: one card, scratch it live; the rainbow stays on bare paper. Click: the before code, Shine() is its own layer on top of Foil, Clear runs inside Foil's layer, so it never erases the shine. Click: the code morphs, Shine() is gone and Foil paints holoBrush instead of Silver. Click: the fixed card appears under it, same finger on both. Reset clears both.

- **The Book of Shaders.** Free, by Patricio Gonzalez Vivo and Jen Lowe. QR to thebookofshaders.com for anyone who wants to learn shaders properly.

## Make it react to your hand
- **How do you hold it?** Title only. Click: two real holo foils in a hand, the coupon and the DevFest Wrocław sticker. Say: "Tilt a real foil card: the reflection slides, the colours shift. So our card must know how you hold the phone." Click: "So far the mouse made roll and pitch. On a phone, your hand does." Say: "The foil already reacts to the mouse. Now we take the same two numbers, −1 to 1, from the phone: roll is tipped left or right, pitch is tipped toward you or away." Click: gravity as a picture. The phone rolls on its own; the mouse does nothing here. Say: "A phone has no angle sensor. It only feels gravity. Gravity always points down, so how it splits over the phone's sides tells us roll and pitch. Look: the phone turns, the orange arrow never moves." Click: the phone recording.
- **Sensor build.** Listener → TYPE_GRAVITY → event.values: x, y, z in m/s² along the phone's axes, with a 3D phone that follows them and the table (flat 0/0/9.8, upright 0/9.8/0, in your hand 0/7.7/6.1) → roll = x / 9.81 → pitch = z / 9.81, about 0.62 in your hand → − 0.62f: the side view, a phone in a hand swings from upright to flat and settles at ~38° back, where z / 9.81 ≈ 0.62. Subtract it so the normal hold reads 0 → × 2.2f: after the subtraction, flat is only 0.38, while rolling reaches ±1, so tipping would barely move the light. 2.2 is a gain tuned by feel: it makes tipping as strong as rolling. The table: normal hold 0 → 0, tip 20° back 0.23 → 0.50, flat 0.38 → 0.84. The app clamps the result to −1..1.
- **Smoothing.** Raw values jitter on a flat card. Click: why: a hand is never still, the MEMS chip adds electrical noise, and it reports ~50 times a second. Click: one line of smoothing and the smoothed card. Click: 0.01 (very late), 0.10, 0.6 (shaky) side by side. The number is how far each reading moves toward the new value: noise flips direction and cancels out, a real tilt repeats and adds up. The app uses 0.18.
- **graphicsLayer.** Flat → raw roll/pitch: rotation is in degrees, so ±1° and nothing moves → two sliders, degrees and cameraDistance: drag them live (try 35°, try camera 3) → the values the app uses: 12° and 14. The card wears the holo foil from the shader chapter.

**Try it on your phone** slides (QR to the web demos on GitHub Pages, https://piotrprus.github.io/holographic-lottery-coupon-agsl/): simple scratch after change.historical, gravity sensor after the sensor build, foil after graphicsLayer, coin after "One more bug", the whole card after the finale. Pages have no links to each other and opaque URLs, so nobody jumps ahead. iPhones need a tap on "Enable motion"; iPhones never vibrate.

## Every scratch leaves crumbs
- **ParticleEmitter.** My library, three effects from the particle talk running live: matrix rain, fire, magic wand. Click: the QR code to github.com/PiotrPrus/ParticleEmitter.
- **CanvasEmitterConfig.** A plain dark stage; the emitter sits under the pointer (lower middle when the pointer is outside). One idea per click: rate + position (white dots, everywhere) → colors, sizes → spread (0° = up) and initialForce: a fountain → lifespan and fade → gravity: the fountain falls back.
- **On the card.** The same config on the scratch card, following the finger. Use reset to clear the card. No sound yet.
  - README sample: a confetti party 🎉.
  - Metal colours.
  - Spread.
  - Gravity.
- **Spot the bug.** Scratch live and ask the room what is wrong. Click: dust from bare paper. Reset under the card.
- **"Is there still foil here?"** Ask the Path ✗ → read the pixel ✗ → keep notes ✓.
- **BooleanArray.** Graph paper under the foil. Scratch and watch the cyan cells. It counts newly cleared cells, so hold that thought.
- **Fresh foil?** One brush move on the graph paper. Click: idea #1 counts foil under the whole brush: 2 of 22 here, near 0 in a live drag, because the brush sits on its own trail. Click: idea #2 counts only the cells this move cleared, out of what it could clear: about 1 on new foil, 0 on bare paper. Click: that number drives the dust. Meters and reset on the right.
- **Before / after.** Same finger, two panels.

## Nobody scratches with a marker
Every scratch before this chapter uses a round brush. The blade appears here first.
- **Stroke → Path.** A round marker vs the shape of a coin.
- **The coin turns.** Scratch a diagonal on both. Reset clears both panels.
- **Hands wobble.** Perfect → random every event (a saw) → random every 9 px (a scrape). Five numbers. Reset.
- **One more bug.** Press, then drag sideways and look at the start. Click: the code. Click: the fix, waiting for the first move. Reset.

## It sounds like foil
- **The sound slide.** The waveform of the recording; play it with "▶ whole file". Click: first try, loop it while dust comes out: it started late, and 2 s didn't match a short flick. Click: zoom into the first 400 ms: 0–70 ms silence, then a slow rise until ~200 ms. Click: trim the start and the fade-out, cut the loud middle into 16 grains of 120 ms ("▶ one grain"). Click: one grain per 36 px of fresh-foil travel; scratch the live card with sound on.

## Outro
- **Finale.** Everything together, with sound. Next slide: the QR code to the same card on their phones.
- **Recap.** One line per click: what it does → what made it happen.
- **Thank you.** The QR code links to the repo.
