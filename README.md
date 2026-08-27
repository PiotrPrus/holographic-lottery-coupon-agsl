# Holographic lottery coupon (AGSL)

A pet project: a fake "Scratch & Win" lottery coupon whose scratch-off panel is rendered
with an **AGSL `RuntimeShader`** simulating brushed aluminium with a holographic
diffraction sheen. Tilt the phone and the sheen band, anisotropic highlight and glitter
facets move as if light were reflecting off real foil. The card itself leans in 3D
perspective with the device.

The sheen is driven entirely by the gravity sensor; touch does one thing only — **scratch
the foil off**. Drag a finger across the panel and the aluminium is removed along the
stroke, taking its holographic sheen with it and revealing the prize printed underneath.
Whatever foil is left keeps reacting to tilt.

Inspired by [Msaneakhtar's write-up on building a holographic Pokémon card on Android](https://medium.com/),
which does it with Canvas layers and blend modes; this takes the single-shader route instead.

## How it works

| File | Role |
|---|---|
| `sensor/DeviceTilt.kt` | `rememberDeviceTilt()` — `TYPE_GRAVITY` (accelerometer fallback), low-pass smoothed into `Tilt(roll, pitch)` in ~[-1, 1]. Registered only while RESUMED. |
| `holo/HoloFoilShader.kt` | The AGSL source. Two uniforms: `uResolution`, `uTilt`. |
| `holo/HoloFoilPanel.kt` | Compose box filled with the shader. Tilt is read inside `onDrawWithContent`, so sensor updates redraw without recomposing. Owns the scratch gesture. |
| `holo/ScratchState.kt` | Accumulates finger strokes into a `Path`, with a `revision` counter as the draw-phase invalidation signal. Also publishes the live fingertip position for the dust. |
| `holo/ScratchDust.kt` | Aluminium flecks thrown off under the finger, via [ParticleEmitter](https://github.com/PiotrPrus/ParticleEmitter). |
| `ui/CouponScreen.kt` | The coupon card + `graphicsLayer` perspective tilt + a roll/pitch readout. |

### Shader layers

Roughly mirroring the physical stack of a real foil card, but in a single pass:

1. **Brushed grain** — three octaves of value noise stretched ~200× along X → long aluminium streaks.
2. **Virtual lamp** — a light position that slides with roll/pitch.
3. **Anisotropic specular** — highlight stretched along the brush direction (real brushed metal does this), plus a wide horizontal sheen band.
4. **Diffraction fringes** — two counter-moving spectral gradients whose phase is a function of position and viewing angle, shown only inside the sheen band so the rest stays gray metal. The palette is blended 55% toward mid-gray so hues read as metallic sheen rather than printed ink.
5. **Glitter facets** — jittered micro-cells, each with a random facet angle; a cell flashes only when its angle aligns with the current tilt, so sparkle twinkles as you move.
6. **Plastic glare** — soft white wash on top, separate from the foil colour.

### Scratching

The panel renders into an offscreen layer (`CompositingStrategy.Offscreen`), and the stroke
path is then drawn over it with `BlendMode.Clear`. Compositing offscreen is what makes
`Clear` punch actual holes instead of painting black — the same isolated-buffer rule that
governs `saveLayer`.

Because the shader is drawn *inside* that layer, an erased region loses the metal and its
sheen in one operation; there is no separate "holo mask" to keep in sync. Coalesced pointer
events are replayed (`change.historical`) so fast swipes don't leave gaps, and a plain tap
writes a zero-length segment that the round cap turns into a dot.

### The scratching tool

People scratch a card with a coin edge or a fingernail, so the eraser is a **thin rectangle**,
not a round brush. Its long axis is kept perpendicular to the direction of travel, so a drag
sweeps a flat-sided band with square ends and a tap leaves a straight-edged scrape rather than a
dot. `BladeLength` (26.dp, across the travel) and `BladeThickness` (8.dp, along it) size it.

The stroke path is therefore **filled, not stroked** — a stroke is always a capsule of constant
width with the caps it is given, which is exactly the rounded shape being avoided. Each movement
appends the parallelogram the blade swept plus the blade itself at the leading end.

A press cannot know which way the blade faces yet, so its geometry is deferred until the first
move reveals the direction; stamping it eagerly left a tab sticking out sideways at the start of
every scrape. A press that never moves is a genuine tap, and lands the blade flat on finger-up.

### Scratch dust

Real foil sheds grit as it comes off, so scratching emits particles under the fingertip using
[ParticleEmitter](https://github.com/PiotrPrus/ParticleEmitter)'s `CanvasParticleEmitter`.

`ScratchState` publishes the live fingertip position; `ScratchDust` is the only composable that
reads it, so the per-frame position updates recompose the emitter config and nothing else. The
emitter captures its config with `rememberUpdatedState`, which means moving `emitterCenter` every
frame makes the source follow the finger. Emission is switched off by setting
`particlePerSecond = 0` on finger-up rather than by removing the emitter, so flecks already in the
air finish falling.

Particles are born along the rim of a `Shape.RECT` matching the blade's contact patch — the
region can't be rotated to follow the drag, but at this size that isn't visible — flicked outward across the full 360° and pulled down hard (`gravityStrength = 900f`).
The overlay sits *above* the foil and *outside* its rounded clip, so dust can spill onto the
ticket instead of being cut off at the panel edge.

**Dust only comes off foil that is still there.** Dragging back across an area you already
scratched emits nothing, and holding a finger still stops emission rather than piling up flecks.

`ScratchState` keeps a coarse boolean occupancy grid (10 px cells) alongside the stroke path —
the cheap way to answer "is there still foil here?", since hit-testing the `Path` would mean
building a `Region` every frame. Each movement reports how many cells it *newly* cleared,
divided by how many that movement could have cleared had everything been virgin foil
(a blade of length `L` sweeping distance `d` clears at most an `L x d` band). Untouched foil scores ~1 at any
drag speed; bare ticket scores 0.

Measuring swept area rather than "how much foil is under the blade" matters: on a slow drag the
blade mostly overlaps what it cleared microseconds ago, so the coverage reading sits
near 3% even on virgin foil — enough to gate off legitimate dust. The ratio is smoothed
(`FreshnessSmoothing`) because a single move only spans a few grid cells, and the slight lag reads
naturally, since dust doesn't stop dead the instant the finger crosses onto bare ticket.

### Perspective tilt

`graphicsLayer { rotationY = roll * 12f; rotationX = -pitch * 12f; cameraDistance = 14f * density }`.
Rolling the right edge down brings it toward the viewer, so it renders slightly larger —
the near edge grows, the far edge shrinks.

## Tuning

All in `HoloFoilShader.kt`:

- **Hue velocity** — the `roll *` / `pitch *` coefficients in `phase1` / `phase2`. Each unit ≈ one full spectrum cycle across the tilt range; currently 0.6/0.9 and 1.0/0.5 (deliberately slow — a small wrist move should shift hues by a shade, not race through the rainbow).
- **Light travel** — the multipliers in the `light` line.
- **Band width** — `exp(-pow((p.y - light.y) * 2.6, 2.0))`; higher = narrower sheen, more plain metal.
- **Colour strength** — the `mix(float3(0.5), pure, 0.55)` in `spectrum()`, and the foil mix weights.
- **Glitter** — cell density `p * 170.0`, rarity `step(0.8, …)`, twinkle rate in `align`.

In `CouponScreen.kt`: `MaxTiltDegrees` (12° is subtle, 18–20° dramatic) and `cameraDistance`
(lower = stronger perspective).

## Running

minSdk 33 — `RuntimeShader` requires Android 13.

```
./gradlew installDebug
```

Best on a real device. On an emulator you can fake tilt with:

```
adb emu sensor set acceleration 4:6:6.5
```
