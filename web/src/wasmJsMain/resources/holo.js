// Browser glue for the Kotlin/Wasm demos. Kotlin reads these globals once per frame
// (see Js.kt), so no callbacks cross the Wasm boundary.
(function () {
  'use strict';

  var isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent) ||
    (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);

  // ── Motion ─────────────────────────────────────────────────────────────────────────────
  //
  // Kotlin wants the same vector Android's TYPE_GRAVITY gives: device axes (x right, y up the
  // screen, z out of the screen), pointing *up*, in m/s². Lying flat face up → (0, 0, +9.81),
  // held upright in portrait → (0, +9.81, 0).
  //
  // DeviceMotionEvent.accelerationIncludingGravity uses the same axes. The spec, and Chrome on
  // Android, report it with the same sign as Android. Safari on iOS reports the opposite sign
  // (flat face up → z = −9.81). So the raw vector is multiplied by `sign`:
  //   1. If deviceorientation is available, the sign is measured, not guessed: beta/gamma give
  //      the up vector (−sinγ·cosβ, sinβ, cosγ·cosβ); if the raw vector points against it, flip.
  //   2. Otherwise fall back to the user agent: −1 on iOS, +1 everywhere else.
  // The vector also contains hand acceleration (it is the raw accelerometer, like Android's
  // TYPE_ACCELEROMETER fallback); the 0.18 smoothing in Kotlin takes the jitter out.
  var m = window.holoMotion = {
    x: 0, y: 0, z: 0,          // Android convention, m/s²
    rawX: 0, rawY: 0, rawZ: 0, // exactly what the browser reported
    sign: isIOS ? -1 : 1,
    signSource: 'user agent',
    events: 0,
    state: 'waiting',          // unsupported | needs-permission | waiting | denied
    upX: NaN, upY: NaN, upZ: NaN
  };

  var hasMotion = typeof window.DeviceMotionEvent !== 'undefined';
  var needsPermission = hasMotion && typeof DeviceMotionEvent.requestPermission === 'function';
  if (!hasMotion) m.state = 'unsupported';
  else if (needsPermission) m.state = 'needs-permission';

  function onOrientation(e) {
    if (e.beta == null || e.gamma == null) return;
    var b = e.beta * Math.PI / 180, g = e.gamma * Math.PI / 180;
    m.upX = -Math.sin(g) * Math.cos(b);
    m.upY = Math.sin(b);
    m.upZ = Math.cos(g) * Math.cos(b);
  }

  function onMotion(e) {
    var a = e.accelerationIncludingGravity;
    if (!a || a.x == null || a.y == null || a.z == null) return;
    m.rawX = a.x; m.rawY = a.y; m.rawZ = a.z;
    var len = Math.sqrt(a.x * a.x + a.y * a.y + a.z * a.z);
    if (!isNaN(m.upX) && len > 4) {
      var cos = (a.x * m.upX + a.y * m.upY + a.z * m.upZ) / len;
      // Only trust a clear answer; a shaking hand can blur it.
      if (cos > 0.7) { m.sign = 1; m.signSource = 'measured'; }
      else if (cos < -0.7) { m.sign = -1; m.signSource = 'measured'; }
    }
    m.x = m.sign * a.x; m.y = m.sign * a.y; m.z = m.sign * a.z;
    m.events++;
  }

  var listening = false;
  function listen() {
    if (listening) return;
    listening = true;
    window.addEventListener('devicemotion', onMotion);
    window.addEventListener('deviceorientation', onOrientation);
  }
  if (hasMotion && !needsPermission) listen();

  // iOS 13+: DeviceMotionEvent.requestPermission() must run inside a user gesture. Any tap
  // anywhere on the page asks (the "Enable motion" button is just an obvious place to tap).
  function requestMotion() {
    if (m.state !== 'needs-permission') return;
    m.state = 'waiting';
    var asks = [DeviceMotionEvent.requestPermission()];
    if (window.DeviceOrientationEvent && typeof DeviceOrientationEvent.requestPermission === 'function') {
      asks.push(DeviceOrientationEvent.requestPermission().catch(function () { return 'denied'; }));
    }
    Promise.all(asks).then(function (r) {
      if (r[0] === 'granted') listen(); else m.state = 'denied';
    }).catch(function () {
      // Not in a gesture, or not HTTPS. Let the next tap try again.
      m.state = 'needs-permission';
    });
  }

  // ── Audio ──────────────────────────────────────────────────────────────────────────────
  //
  // Web Audio, not <audio>: grains are ~120 ms and must start without latency. The context can
  // only start inside a user gesture, so it is created/resumed on the first tap.
  var GRAINS = 16;
  var MAX_VOICES = 4; // like SoundPool.setMaxStreams(4)
  var A = window.holoAudio = { ctx: null, buffers: [], lastIndex: -1, voices: 0, wanted: false };

  function loadGrains() {
    for (var i = 0; i < GRAINS; i++) {
      (function (i) {
        var name = 'sounds/scratch_grain_' + (i < 10 ? '0' : '') + i + '.wav';
        fetch(name)
          .then(function (r) { return r.arrayBuffer(); })
          .then(function (b) { return new Promise(function (ok, fail) { A.ctx.decodeAudioData(b, ok, fail); }); })
          .then(function (buf) { A.buffers.push(buf); })
          .catch(function (err) { console.warn('grain ' + name + ' failed', err); });
      })(i);
    }
  }

  function unlockAudio() {
    if (!A.wanted) return;
    if (!A.ctx) {
      var Ctx = window.AudioContext || window.webkitAudioContext;
      if (!Ctx) return;
      A.ctx = new Ctx();
      loadGrains();
    }
    if (A.ctx.state !== 'running') A.ctx.resume();
    // iOS only really unlocks once something has been played inside the gesture.
    var silent = A.ctx.createBuffer(1, 1, 22050);
    var src = A.ctx.createBufferSource();
    src.buffer = silent; src.connect(A.ctx.destination); src.start(0);
  }

  A.want = function () { A.wanted = true; };
  A.isRunning = function () { return !!A.ctx && A.ctx.state === 'running' && A.buffers.length > 0; };
  A.play = function (volume, rate) {
    if (!A.ctx || A.ctx.state !== 'running' || A.buffers.length === 0 || A.voices >= MAX_VOICES) return;
    var n = A.buffers.length;
    var index = Math.floor(Math.random() * n);
    if (index === A.lastIndex && n > 1) index = (index + 1) % n;
    A.lastIndex = index;
    var src = A.ctx.createBufferSource();
    src.buffer = A.buffers[index];
    src.playbackRate.value = rate;
    var gain = A.ctx.createGain();
    gain.gain.value = volume;
    src.connect(gain); gain.connect(A.ctx.destination);
    A.voices++;
    src.onended = function () { A.voices--; };
    src.start();
  };

  // ── Vibration ──────────────────────────────────────────────────────────────────────────
  // Android Chrome only. iOS Safari has no navigator.vibrate, so this silently does nothing.
  window.holoVibrate = function (ms) {
    try { if (navigator.vibrate) navigator.vibrate(ms); } catch (e) { /* ignore */ }
  };

  // ── First-gesture hook ─────────────────────────────────────────────────────────────────
  // Capture phase on document, so it runs before the Compose canvas sees (and maybe consumes)
  // the event. touchend / pointerup / click are all "user activation" events.
  function onGesture() {
    requestMotion();
    unlockAudio();
  }
  ['touchend', 'pointerup', 'click', 'keydown'].forEach(function (type) {
    document.addEventListener(type, onGesture, { capture: true, passive: true });
  });

  // ── Boot ───────────────────────────────────────────────────────────────────────────────
  window.holoBooted = function () {
    var boot = document.getElementById('boot');
    if (boot) boot.remove();
  };
})();
