# Holographic lottery coupon (AGSL)

A pet project: a fake "Scratch & Win" lottery coupon whose scratch-off panel is rendered
with an **AGSL `RuntimeShader`** simulating brushed aluminium with a holographic
diffraction sheen. Tilt the phone and the sheen band, anisotropic highlight and glitter
facets move as if light were reflecting off real foil. The card itself leans in 3D
perspective with the device.

Everything is driven by the gravity sensor — there is **no touch interaction**, and no
scratching/reveal logic. The point is the material, not the game.

Inspired by [Msaneakhtar's write-up on building a holographic Pokémon card on Android](https://medium.com/),
which does it with Canvas layers and blend modes; this takes the single-shader route instead.

## How it works

| File | Role |
|---|---|
| `sensor/DeviceTilt.kt` | `rememberDeviceTilt()` — `TYPE_GRAVITY` (accelerometer fallback), low-pass smoothed into `Tilt(roll, pitch)` in ~[-1, 1]. Registered only while RESUMED. |
| `holo/HoloFoilShader.kt` | The AGSL source. Two uniforms: `uResolution`, `uTilt`. |
| `holo/HoloFoilPanel.kt` | Compose box filled with the shader. Tilt is read inside `onDrawBehind`, so sensor updates redraw without recomposing. |
| `ui/CouponScreen.kt` | The coupon card + `graphicsLayer` perspective tilt + a roll/pitch readout. |

### Shader layers

Roughly mirroring the physical stack of a real foil card, but in a single pass:

1. **Brushed grain** — three octaves of value noise stretched ~200× along X → long aluminium streaks.
2. **Virtual lamp** — a light position that slides with roll/pitch.
3. **Anisotropic specular** — highlight stretched along the brush direction (real brushed metal does this), plus a wide horizontal sheen band.
4. **Diffraction fringes** — two counter-moving spectral gradients whose phase is a function of position and viewing angle, shown only inside the sheen band so the rest stays gray metal. The palette is blended 55% toward mid-gray so hues read as metallic sheen rather than printed ink.
5. **Glitter facets** — jittered micro-cells, each with a random facet angle; a cell flashes only when its angle aligns with the current tilt, so sparkle twinkles as you move.
6. **Plastic glare** — soft white wash on top, separate from the foil colour.

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
