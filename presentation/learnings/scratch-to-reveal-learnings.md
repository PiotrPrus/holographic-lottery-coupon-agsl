# Scratch to Reveal: Study Guide

**Building a holographic scratch card with Jetpack Compose, AGSL shaders, the gravity sensor and particles**

Source talk: "Scratch to Reveal", Piotr Prus (TILT), DevFest Wrocław 2026.
Code: https://github.com/PiotrPrus/holographic-lottery-coupon-agsl

This guide holds only the learnings from the talk. Each chapter states the problem, the technique that solves it, the code, and the mistakes made along the way. Code is Kotlin (Jetpack Compose) and AGSL (Android Graphics Shading Language).

---

## Overview: what makes a scratch card feel real?

The talk asks one question: can a lottery ticket on a phone screen feel like real metal? It answers with five ingredients. Each ingredient is one chapter. The lean is part of the sensor chapter.

| Ingredient | What gives it |
|---|---|
| It scratches | A layer you can rub away: `graphicsLayer` with `CompositingStrategy.Offscreen` plus `BlendMode.Clear` |
| It knows how you hold it | The gravity sensor (`Sensor.TYPE_GRAVITY`) with a little smoothing |
| It leans | `graphicsLayer` `rotationX` / `rotationY` and `cameraDistance` |
| It shines like foil | An AGSL shader (`RuntimeShader`) driven by uniforms |
| It leaves crumbs | Particles (`CanvasParticleEmitter`) plus an occupancy grid ("graph paper") |
| It scrapes like a coin | A filled `Path` shaped like a blade that turns and wobbles, not a round brush |

The teaching method: show the finished effect first, then build it step by step. The code grows a few lines at a time, and every step has a visible result.

---

## Chapter 1: It scratches (a layer you can rub away)

### Goal
Draw a prize. Cover it with grey foil. Let the finger remove the foil where it touches.

### Step by step

1. **The prize.** A `Box` that shows the prize, for example "YOU WON $100".
2. **The foil.** A second composable on top that draws a grey rectangle in `drawWithCache { onDrawWithContent { drawRect(Silver) } }`.
3. **Follow the finger.** Add `pointerInput` with `detectDragGestures`. Store the finger positions in a `Path`.
   - First attempt: draw the path in a dark colour. Result: it looks like a pen line on top of the foil. That is a pen, not a coin.
4. **Erase, do not paint.** Draw the path with `color = Color.Transparent` and `blendMode = BlendMode.Clear`.
   - Problem: `BlendMode.Clear` clears everything under it, including the prize and the window background. You get a black hole "through everything".
5. **Give the foil its own layer.** Add `graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }` to the foil.
   - Now the foil renders into its own offscreen buffer first. `BlendMode.Clear` clears only pixels of that buffer. Then the buffer is drawn over the prize. The hole shows the prize. This is the key trick.

Official KDoc for `CompositingStrategy.Offscreen`: "Rendering of content will always be rendered into an offscreen buffer first, then drawn to the destination regardless of the other parameters configured on the graphics layer. This is useful for leveraging different blending algorithms for masking content."

### Final code shape

```kotlin
Box {
    Prize()   // "YOU WON $100"
    Foil(scratch)
}

@Composable
fun Foil(scratch: ScratchState) = Box(
    Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { scratch.start(it) },
                onDrag = { drag, _ -> scratch.extend(drag.position) },
            )
        }
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .drawWithCache {
            onDrawWithContent {
                drawRect(Silver)
                drawPath(
                    path = scratch.path,
                    color = Color.Transparent,
                    style = coinStroke,
                    blendMode = BlendMode.Clear,
                )
            }
        }
)
```

### Learning: a `Path` changes silently (`mutableIntStateOf`)
A `Path` is a plain mutable object. Calling `path.lineTo(...)` does not tell Compose anything. Result: you scratch and nothing shows up.

Fix: keep a tiny counter in state and bump it on every change. Read the counter inside the draw lambda. Compose then reruns only the drawing when the counter changes. There is no recomposition and no layout, only a redraw.

```kotlin
class ScratchState {
    val path = Path()
    var revision by mutableIntStateOf(0)

    fun extend(position: Offset) {
        path.lineTo(position.x, position.y)
        revision++   // "hey Compose, I changed"
    }
}

onDrawWithContent {
    scratch.revision   // read it here -> redraw when it changes
    drawPath(scratch.path, ...)
}
```

General rule: state read in the draw phase invalidates only the draw phase. This is the cheapest way to animate in Compose.

### Learning: fast swipes come in batches (`change.historical`)
Android delivers touch input once per frame. A fast swipe moves far in one frame. The system batches the points between frames into `PointerInputChange.historical`. If you use only `change.position`, a fast scratch becomes a dotted line.

Fix: replay the historical points before the current one.

```kotlin
awaitEachGesture {
    val down = awaitFirstDown()
    scratch.start(down.position)
    do {
        val event = awaitPointerEvent()
        event.changes.forEach { change ->
            change.historical.forEach { scratch.extend(it.position) }
            scratch.extend(change.position)
        }
    } while (event.changes.any { it.pressed })
}
```

---

## Chapter 2: It knows how you hold it (the gravity sensor)

### What the gravity sensor measures
Gravity always points straight down, whatever the phone does. The phone reports how much of that gravity falls along its own axes:

- `values[0]`: gravity along the phone's X axis (left/right). Changes when you roll the phone.
- `values[1]`: gravity along the phone's Y axis (top/bottom).
- `values[2]`: gravity along the Z axis (out of the screen). Changes when you pitch the phone toward you.

Each value is between about -9.81 and +9.81 m/s². No trigonometry is necessary. Divide by 9.81 and you get a number from -1 to 1. That number is all the shader needs.

`TYPE_GRAVITY` is better than the raw accelerometer: the system already filters out the shake of your hand moving.

### Building `rememberDeviceTilt`

```kotlin
@Composable
fun rememberDeviceTilt(): State<Tilt> {
    val tilt = remember { mutableStateOf(Tilt.Zero) }
    LifecycleResumeEffect(Unit) {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val roll = event.values[0] / 9.81f
                val pitch = (event.values[2] / 9.81f - 0.62f) * 2.2f
                tilt.value = Tilt(roll, pitch)
            }
        }
        sensorManager.registerListener(listener, sensor, SENSOR_DELAY_GAME)
        onPauseOrDispose { sensorManager.unregisterListener(listener) }
    }
    return tilt
}
```

Learnings:
- **Listen only while the screen is on.** `LifecycleResumeEffect` registers the listener on resume and `onPauseOrDispose` unregisters it. This saves battery.
- **`SENSOR_DELAY_GAME`** gives a rate good for animation.
- **Roll** = tip the phone left and right = `values[0] / 9.81`.
- **Pitch** = tip the phone toward you.
- **Nobody holds a phone upright.** People hold a phone at an angle. The constant `0.62` is the typical angle you actually hold it at. Subtracting it makes that pose the new "zero". The factor `2.2` scales the remaining range back to roughly -1..1.

### Smoothing
Real sensors are shaky. Raw values jitter, and the jitter shows directly in the shine. One line of smoothing fixes it: an exponential moving average. Each reading moves the value only a fraction of the way toward the new target.

```kotlin
override fun onSensorChanged(event: SensorEvent) {
    val target = event.values[0] / 9.81f
    roll += (target - roll) * 0.18f   // move 18% of the way
    tilt.value = Tilt(roll, pitch)
}
```

Smaller factor = calmer but slower. Bigger factor = faster but shakier. 0.18 is the tuned value in the app.

### Lean the card with `graphicsLayer`
Rotate the card a little in 3D with the same tilt values.

```kotlin
CouponCard(
    modifier = Modifier.graphicsLayer {
        val t = tilt()
        rotationY = t.roll * 12f
        rotationX = -t.pitch * 12f
        cameraDistance = 14f * density
    }
)
```

Tuning lessons, found by trying the wrong values first:
- 0° (flat): just a picture of a card.
- 12°: like a card lying on a table. Good.
- 35°: far too much. "Whoa, calm down."
- `cameraDistance = 3`: fisheye distortion. Bad.
- 12° with a far camera (14 × density): subtle wins.

Also note: `tilt` is passed as a lambda `() -> Tilt` and read inside `graphicsLayer { }`. The read happens in the draw/layer phase, so sensor ticks do not cause recomposition.

---

## Chapter 3: It shines like foil (AGSL shaders)

### What is a shader?
A tiny program that runs once for every pixel, all of them at the same time, on the GPU. On Android you write shaders in AGSL (Android Graphics Shading Language). AGSL is Skia's SkSL, which is close to GLSL. Android 13 (API 33) and higher supports `RuntimeShader`.

### A first shader in three steps

**1. Every pixel: "be pink".** `half4` is red, green, blue, alpha.

```glsl
half4 main(float2 fragCoord) {
    return half4(1.0, 0.3, 0.6, 1.0);
}
```

**2. Every pixel knows where it is.** `fragCoord` is the pixel position. Divide by the size and you get `uv`, which goes 0 to 1 across the box. Red grows to the right, green grows down.

```glsl
uniform float2 uResolution;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / uResolution;
    return half4(uv.x, uv.y, 1.0, 1.0);
}
```

**3. A uniform is a value we send from Kotlin.** Here the tilt moves a spotlight.

```glsl
uniform float2 uResolution;
uniform float2 uTilt;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / uResolution;
    float2 light = 0.5 + float2(uTilt.x, -uTilt.y) * 0.5;
    float glow = exp(-distance(uv, light) * 6.0);
    return half4(half3(0.35 + glow), 1.0);
}
```

### Plugging the shader into Compose

```kotlin
Modifier.drawWithCache {
    val shader = RuntimeShader(SPOTLIGHT_AGSL)
    val brush = ShaderBrush(shader)
    shader.setFloatUniform("uResolution", size.width, size.height)
    onDrawWithContent {
        val t = tilt()
        shader.setFloatUniform("uTilt", t.roll, t.pitch)
        drawRect(brush)
    }
}
```

Learnings:
- **The shader is just a String.** `RuntimeShader(source)` compiles it.
- **Compile once.** Create the shader in `drawWithCache`, so it lives as long as the size does.
- **Size: once, in the cache.** Set `uResolution` in the cache block, because it changes only when the size changes.
- **Tilt: every frame.** Set `uTilt` inside `onDrawWithContent`. The tilt is read in the draw phase, so the sensor ticks and nothing recomposes.
- Fun fact: Compose Desktop has no `RuntimeShader`. Because AGSL is SkSL, the same source runs on desktop through Skia's `RuntimeEffect` and `RuntimeShaderBuilder`.

### The holographic foil, one layer at a time
The real foil is the same shader, grown layer by layer. Each layer adds a few lines.

```glsl
half4 main(float2 fragCoord) {
    float3 col = float3(0.58, 0.59, 0.62);              // aluminium grey
    float grain = noise(float2(p.x * 5.0, p.y * 1100.0)); // stretched 200x
    col += (grain - 0.5) * 0.11;
    float2 light = float2(0.5 + roll, 0.5 - pitch);      // a lamp that follows tilt
    col += exp(-dot(p - light, p - light) * 7.0) * 0.16;
    float band = exp(-pow((p.y - light.y) * 2.6, 2.0));  // where the light hits
    float phase = p.y * 1.8 + roll * 0.6 - pitch * 0.9;
    col = mix(col, rainbow(phase), band * 0.35);
    float2 cell = floor(p * 170.0);                      // tiny mirrors
    float angle = hash(cell) * 2.0 * PI;                 // each tilted randomly
    col += step(0.96, cos(angle + roll * 7.0)) * 0.5;    // flash when aligned
    col += exp(-dot(p - light, p - light) * 1.4) * 0.06; // plastic glare
    col *= edgeShade(uv);
    return half4(col, 1.0);
}
```

The six layers:

1. **Grey: where everyone starts.** Aluminium grey, `float3(0.58, 0.59, 0.62)`.
2. **Brushed grain: noise, stretched sideways.** Value noise sampled with a very different scale on each axis (`x * 5`, `y * 1100`). Stretched noise looks like brushed metal. The full shader adds three octaves of noise at different stretches.
3. **A lamp: the reflection follows your hand.** A light position that moves with roll and pitch. A Gaussian falloff `exp(-dist² * k)` gives a soft specular highlight. In the full shader the highlight is anisotropic: the x distance is multiplied by 0.28, so the highlight is wide, like on brushed metal.
4. **The rainbow: only where the light hits.** Diffraction foil shows colour only near the reflection. A horizontal `band` around the light limits the effect. The colour comes from a spectrum function `0.5 + 0.5 * cos(2 * PI * (t + (0, 1/3, 2/3)))`, mixed toward grey so it is not too saturated. The phase depends on position and on tilt, so the colour shifts as you tilt.
5. **Glitter: tiny mirrors that flash.** Split the surface into a grid of tiny cells (170 per unit). Each cell gets a random angle from a hash. A cell flashes only when the tilt lines up with its angle: `smoothstep(0.96, 1.0, cos(angle + roll * 7 + pitch * 5.5))`. Only about 20% of cells are glitter.
6. **Glare: the plastic on top.** A wide, faint highlight (the protective plastic) and darker edges (`edgeShade`).

Desktop porting note: `pow(x, 2.0)` is undefined for a negative base. Some GPU backends take that literally. Write `x * x` instead.

### Learning: the oil slick (rainbow speed)
First attempt: `float phase = p.y * 1.8 + roll * 2.0 - pitch * 3.0;`
A tiny wrist turn ran through the whole rainbow. It looked like an oil slick, not foil. It took three builds to find.

Fix: `float phase = p.y * 1.8 + roll * 0.6 - pitch * 0.9;`

Real foil changes colour slowly as you tilt it. The light moves fast, the colour crawls. Smaller numbers give a calmer rainbow.

### Learning: put the shine inside the foil layer
Two options:
- Paint the shine as a separate layer on top of the foil. When you scratch, the foil goes away but the shine stays floating over the prize. It looks fake.
- Draw the shine as the foil itself, in the same offscreen layer. Then `BlendMode.Clear` removes the metal and its rainbow together. This costs nothing extra.

```kotlin
onDrawWithContent {
    drawRect(holoBrush)        // metal + rainbow
    drawContent()
    drawPath(scratch.path, Color.Transparent, blendMode = BlendMode.Clear)
}
```

---

## Chapter 4: It leaves crumbs (particles)

### The emitter
The talk uses the speaker's open source library ParticleEmitter (github.com/PiotrPrus/ParticleEmitter). The emitter follows the finger. The config was grown one idea at a time:

1. **README defaults:** a confetti party. Bright colours, big particles. Wrong for metal.
2. **Colours:** crumbs of metal, not a party. Silver, graphite, steel. Small sizes: 2, 3 and 5 dp.
3. **Spread:** flick them everywhere. Full circle spread, random force, short life.
4. **Gravity:** metal falls.

```kotlin
CanvasParticleEmitter(
    config = CanvasEmitterConfig(
        particlePerSecond = 300,
        emitterCenter = fingerPosition,
        colors = listOf(Silver, Graphite, Steel),
        particleSizes = listOf(2.dp, 3.dp, 5.dp),
        spread = -180..180,
        initialForce = 30..190,
        lifespanRange = 380..900,
        gravityStrength = 1300f,
    ),
)
```

### The bug: dust from bare paper
Scratch an area, then go back over the same spot. Dust still pours out, from paper that has no foil left. The finger makes the dust. The metal should.

So the code must answer: "Is there still foil here?" It must answer about 60 times a second.

### Three answers
- **Ask the Path. Rejected.** A `Path` cannot cheaply answer "am I covering this point?".
- **Read the pixel. Rejected.** The pixels live on the GPU. Reading them back to the CPU is slow.
- **Keep notes. Chosen.** Write down every spot that was scratched, like graph paper under the foil.

### The graph paper: a `BooleanArray`
Split the card into 10 px cells. One boolean per cell: `true` = cleared. When the coin moves from `from` to `to`, mark every cell near that segment. The function returns how many cells were newly cleared.

```kotlin
private const val CellPx = 10f
private val cleared = BooleanArray(cols * rows)

fun clearAlong(from: Offset, to: Offset, radius: Float): Int {
    var newlyCleared = 0
    for (cell in cellsNear(from, to, radius)) {
        if (!cleared[cell]) {
            cleared[cell] = true
            newlyCleared++
        }
    }
    return newlyCleared
}
```

"Cells near the segment" means cells whose centre is within `radius` of the segment (point-to-segment distance). Only cells in the segment's bounding box are checked, so the cost is small.

### Which metric drives the dust?

**Idea #1 (wrong): how much of the coin is on foil?**
`covered = foilUnderCoin / cellsUnderCoin`
Drag slowly over untouched foil and this value sits near zero. Why? The coin overlaps the area it cleared a moment ago. Most cells under the coin are already cleared by the coin itself.

**Idea #2 (right): how much NEW foil did this move remove?**
Only the front edge of the coin touches fresh foil, so count that.

```kotlin
val newlyCleared = clearAlong(last, position, halfLength)
val couldClear = coinWidth * moved / (CellPx * CellPx)
val fresh = newlyCleared / couldClear

particlePerSecond = (300 * fresh).roundToInt()
```

`couldClear` is the number of cells this move would clear on fresh foil (swept area divided by cell area). The ratio is 1 on fresh foil and 0 on bare paper. The app smooths this value a little (factor 0.35), so the dust fades out nicely at the edges of earlier scratches.

Result: fresh foil = full dust. Bare paper = no dust.

---

## Chapter 5: It scrapes like a coin (the blade)

### Stroke vs Path: nobody scratches with a marker
First version: draw the path as a stroke with a round cap.

```kotlin
drawPath(
    path = scratch.path,
    color = Color.Transparent,
    blendMode = BlendMode.Clear,
    style = Stroke(width = 26.dp.toPx(), cap = StrokeCap.Round),
)
```

It is round, fat and the same width everywhere. It looks like a marker. A coin edge is a thin rectangle.

Better: on every move, add the rectangle the coin swept to a path, and draw the path filled (no `style` = `Fill`).

```kotlin
// every move: add the rectangle the coin swept
path.moveTo(from.x + side.x, from.y + side.y)
path.lineTo(to.x + side.x, to.y + side.y)
path.lineTo(to.x - side.x, to.y - side.y)
path.lineTo(from.x - side.x, from.y - side.y)
path.close()

drawPath(
    path = scratch.path,
    color = Color.Transparent,
    blendMode = BlendMode.Clear,   // no style = Fill
)
```

### The coin turns with your hand
If `side` is fixed (`Offset(halfWidth, 0f)`), the coin always lies flat. A diagonal scratch then looks wrong. A real coin edge stays perpendicular to the direction you drag.

```kotlin
val direction = (position - last) / moved                  // where the finger goes
val side = Offset(-direction.y, direction.x) * halfWidth   // 90° to it
```

`(-y, x)` is the direction rotated by 90°.

### Hands wobble
A perfect, constant width looks like a computer drew it. Three tries:
- **Perfect:** too clean.
- **Random width on every pointer event:** saw-blade edges, because events arrive at random distances.
- **New random target every 9 px of travel, eased toward it:** a ragged, real scrape.

The whole "hand" is five numbers:

```kotlin
private const val WidthMin = 0.70f     // the coin is 70%...
private const val WidthMax = 1.20f     // ...to 120% wide
private const val SideDrift = 0.18f    // and drifts sideways a bit
private const val EveryPx = 9f         // pick a new target every 9 px
private const val Follow = 0.35f       // ease toward it, don't jump
```

Learning: tie randomness to distance, not to events. Event timing depends on the device and the speed of the finger. Distance does not.

Implementation details from the engine:
- Consecutive segments are joined as **trapezoids**: the start edge uses the previous width, the end edge uses the new width. Independent rectangles leave visible steps and gaps when the width changes.
- All quads are added with the **same winding** (clockwise). The path uses the non-zero fill rule. A quad wound the other way would cancel an overlapping quad and leave an unscratched stripe.

### One more bug: the "tab" at the start
Press, then drag sideways, and look at the start of the scratch. A blade sits there at the wrong angle.

Cause: on press there is no direction yet, so the code stamped the blade with a default direction.

```kotlin
fun start(position: Offset) {
    addCoin(position, direction = Down)   // which way? no idea yet
}
```

Fix: wait for the first move. Then the direction is known.

```kotlin
fun start(position: Offset) {
    pendingPress = true                     // wait...
}

fun extend(position: Offset) {
    if (pendingPress) addCoin(last, direction)   // ...now we know
}
```

If the finger lifts without moving (a tap), stamp the blade with the default direction on lift.

---

## Recap: what it does and what made it happen

| What it does | What made it happen |
|---|---|
| It scratches | `graphicsLayer` `CompositingStrategy.Offscreen` + `BlendMode.Clear` |
| It knows how you hold it | `TYPE_GRAVITY` + a little smoothing |
| It leans | `graphicsLayer` `rotationX` / `rotationY` |
| It shines | AGSL + `RuntimeShader` + uniforms |
| It leaves crumbs | `CanvasParticleEmitter` + graph paper (`BooleanArray`) |
| It scrapes like a coin | A filled `Path` that turns and wobbles |

## Key principles to remember

1. `BlendMode.Clear` erases everything below it unless the content is in its own offscreen layer. Use `CompositingStrategy.Offscreen` for masking.
2. Mutable objects such as `Path` do not notify Compose. Use a state counter and read it in the draw phase.
3. Read fast-changing values (sensor, tilt) in the draw or layer phase, not in composition. Then only drawing reruns.
4. Replay `change.historical` for smooth fast gestures.
5. Use `TYPE_GRAVITY`, normalise by 9.81, offset pitch by the natural holding angle, and smooth with an exponential moving average.
6. Subtle 3D wins: about 12° of rotation and a far camera distance.
7. A shader runs per pixel on the GPU. Compile it once in `drawWithCache`, set static uniforms in the cache, set changing uniforms every frame.
8. Build a material in layers: base colour, grain, specular, colour band, glitter, glare.
9. Real foil changes colour slowly. Keep tilt factors on the hue small.
10. Put the shine in the same layer as the foil, so scratching removes both.
11. Never read GPU pixels to answer gameplay questions. Keep a cheap CPU-side grid.
12. Measure what changed (newly cleared cells), not the current state (cells under the tool).
13. Shape the tool like the real object: a turning blade, not a round brush.
14. Tie randomness to distance travelled, not to input events.
15. Do not commit to a direction before you have one: defer the first stamp until the first move.

## Self-test questions

1. Why does `BlendMode.Clear` alone create a black hole instead of revealing the prize?
2. What does `CompositingStrategy.Offscreen` change?
3. Why does nothing appear on screen when you only call `path.lineTo` in a drag handler?
4. What is in `PointerInputChange.historical` and when does it matter?
5. What does `values[0]` of `TYPE_GRAVITY` measure, and why divide by 9.81?
6. What is the 0.62 in the pitch formula?
7. Write the one-line exponential smoothing formula.
8. Why does too large a `rotationY` or too small a `cameraDistance` look bad?
9. What is a uniform? Which uniform is set once and which every frame?
10. Why is the rainbow limited to a band near the light?
11. How does the glitter layer decide which cells flash?
12. What caused the "oil slick" look, and what fixed it?
13. Why must the shine be drawn in the same layer as the foil?
14. Why not read pixels from the GPU to find out if foil remains?
15. Why does "fraction of the coin over foil" stay near zero during a slow drag on fresh foil?
16. How is the "fresh foil" ratio computed?
17. Why does random width per pointer event look like a saw blade?
18. How do you compute a vector perpendicular to the drag direction?
19. Why must all quads in the path have the same winding?
20. What causes the "tab" at the start of a stroke, and how is it fixed?

## Glossary

- **AGSL**: Android Graphics Shading Language. Shader language for `RuntimeShader` on Android 13+. Equal to Skia's SkSL.
- **BlendMode.Clear**: blend mode that sets destination pixels to fully transparent.
- **CompositingStrategy.Offscreen**: makes a `graphicsLayer` render into its own buffer before compositing.
- **drawWithCache**: draw modifier that keeps objects (shaders, brushes) until the size or read state changes.
- **Uniform**: a value sent from app code to a shader, equal for every pixel.
- **uv**: pixel position normalised to 0..1.
- **Specular highlight**: the bright reflection of a light source.
- **Anisotropic**: different along each axis, as in brushed metal.
- **Diffraction foil**: foil with micro-structure that splits light into a rainbow.
- **Hash**: function that turns a cell coordinate into a stable pseudo-random number.
- **Exponential moving average**: `value += (target - value) * k`.
- **Occupancy grid**: a grid of booleans that records which cells are cleared.
- **Winding / non-zero fill rule**: path fill rule where opposite-direction overlapping shapes can cancel each other.
