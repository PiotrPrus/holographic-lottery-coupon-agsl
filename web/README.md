# Web demos

The scratch card in the browser, so the audience can try it on their own phones during the talk.
Compose Multiplatform for **Kotlin/Wasm**: Compose draws with Skia on the web too, so the AGSL
foil shader runs unchanged as SkSL (`RuntimeEffect`), and `ScratchState` is the app's file
copied over.

## Pages

Base URL: <https://piotrprus.github.io/holographic-lottery-coupon-agsl/>

| # | Page | URL |
|---|------|-----|
|   | Landing (no list) | `…/holographic-lottery-coupon-agsl/` |
| 1 | Simple scratch (grey paint, round pen, `BlendMode.Clear`) | `…/#/k7p2qx` |
| 2 | Gravity sensor check (x, y, z, roll, pitch, arrow down) | `…/#/m3v8ra` |
| 3 | Foil (holo shader driven by tilt) | `…/#/t9c4wd` |
| 4 | Coin scratch (holo foil + coin blade) | `…/#/h2n6zb` |
| 5 | Final (coin + foil + dust + sound + vibration) | `…/#/q5r1fy` |

The keys are opaque on purpose and pages have no links to each other: the audience gets each demo only from its QR code in the talk. The bare site shows a landing page with no list. Any unknown hash shows the landing page.

Hash routes, so every page can have its own QR code and GitHub Pages never 404s on reload.

## Run locally

```bash
./gradlew :web:wasmJsBrowserDevelopmentRun          # dev server, opens the browser
./gradlew :web:wasmJsBrowserDistribution            # release bundle
cd web/build/dist/wasmJs/productionExecutable && python3 -m http.server 8080
```

On a laptop there is no motion sensor: moving the mouse tilts instead.

**On a phone:** motion sensors need a secure context (HTTPS or `localhost`), so
`http://<laptop-ip>:8080` shows no tilt. For an Android phone on USB, use
`adb reverse tcp:8080 tcp:8080` and open `http://localhost:8080` on the phone. Otherwise test
on the deployed HTTPS site.

If the Kotlin compiler fails with an `ArrayIndexOutOfBoundsException` in `WasmIrFileMetadata`,
that's an incremental-compilation glitch: run `./gradlew :web:clean` and build again.

## Phones

- **Browser:** needs WebAssembly GC: Chrome 119+, Firefox 120+, Safari on **iOS 18.2+**.
- **iPhone motion permission:** iOS asks before a page may read the motion sensor, and only
  allows the question from a tap. Pages that use tilt show an **Enable motion** button; any
  tap on the page triggers the prompt (`DeviceMotionEvent.requestPermission()` in `holo.js`).
  If someone taps *Don't Allow*, they need to reload the page.
- **Sound** starts on the first tap (browsers block audio until then). On iPhone the
  ring/silent switch must be on ring.
- **Vibration** uses `navigator.vibrate`, Android only. iPhones have no vibration API, so they
  get sound only.

## Deploy (one-time setup)

The workflow `.github/workflows/web-pages.yml` builds and deploys on every push to `main`
(and by hand from the Actions tab). In the repo, once:

**Settings → Pages → Build and deployment → Source: GitHub Actions.**

## Files

- `src/wasmJsMain/resources/index.html`: page shell, viewport, "Loading…" placeholder.
- `src/wasmJsMain/resources/holo.js`: browser APIs in plain JS: motion (with the
  iOS sign fix), Web Audio grains, vibration, the first-tap hook.
- `Js.kt`: the Kotlin side of that bridge. `Tilt.kt`: the app's roll/pitch formula.
- `HoloShader.kt`, `HoloFoilPanel.kt`, `CoinScratchState.kt`, `ScratchSound.kt`, `ScratchDust.kt`:
  ports of the app's files. The dust uses ParticleEmitter (`particle-emitter-wasm-js`), only on
  the Final page, and only where fresh foil comes off.

The release bundle has no source map (`devtool` is off for production in `build.gradle.kts`).
