package com.example.holoscratch.holo

import org.intellij.lang.annotations.Language

/**
 * AGSL shader that renders a brushed-aluminium scratch panel with a holographic
 * diffraction-grating sheen. Everything is driven by two uniforms:
 *
 *  uResolution — panel size in px
 *  uTilt       — (roll, pitch) in roughly [-1, 1], from the device gravity vector
 *
 * Layer model (mirrors the physical foil stack):
 *  1. brushed grain — long anisotropic streaks along X, three noise octaves
 *  2. light position — a virtual overhead lamp whose reflection slides with tilt
 *  3. anisotropic specular — highlight stretched along the brush direction
 *  4. diffraction fringes — two counter-moving spectral bands, phase = f(position, tilt)
 *  5. glitter — per-cell facets that flash when their random angle aligns with the tilt
 *  6. plastic glare — soft white wash on top, separate from the foil colour
 */
@Language("AGSL")
val HOLO_FOIL_SHADER =
  """
  uniform float2 uResolution;
  uniform float2 uTilt;

  const float PI = 3.14159265;

  float hash21(float2 p) {
      p = fract(p * float2(123.34, 456.21));
      p += dot(p, p + 45.32);
      return fract(p.x * p.y);
  }

  float vnoise(float2 p) {
      float2 i = floor(p);
      float2 f = fract(p);
      float2 u = f * f * (3.0 - 2.0 * f);
      float a = hash21(i);
      float b = hash21(i + float2(1.0, 0.0));
      float c = hash21(i + float2(0.0, 1.0));
      float d = hash21(i + float2(1.0, 1.0));
      return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
  }

  // Cheap spectral palette: t wraps 0..1 through red -> green -> blue -> red,
  // desaturated toward mid-gray so it reads as a metallic sheen, not printed ink.
  float3 spectrum(float t) {
      float3 pure = 0.5 + 0.5 * cos(2.0 * PI * (t + float3(0.0, 0.333, 0.667)));
      return mix(float3(0.5), pure, 0.55);
  }

  half4 main(float2 fragCoord) {
      float2 uv = fragCoord / uResolution;
      float aspect = uResolution.x / uResolution.y;
      float2 p = float2(uv.x * aspect, uv.y);

      float roll = uTilt.x;
      float pitch = uTilt.y;

      // ── 1. brushed grain ───────────────────────────────────────────────
      float grain = 0.0;
      grain += vnoise(float2(p.x * 5.0,  p.y * 1100.0)) * 0.50;
      grain += vnoise(float2(p.x * 16.0, p.y * 380.0))  * 0.32;
      grain += vnoise(float2(p.x * 55.0, p.y * 140.0))  * 0.18;
      grain -= 0.5;

      float3 col = float3(0.58, 0.59, 0.62) + grain * 0.11;

      // ── 2. light position ─────────────────────────────────────────────
      float2 light = float2(0.5 * aspect + roll * 1.1 * aspect, 0.5 - pitch * 1.05);

      // ── 3. anisotropic specular ───────────────────────────────────────
      float2 d = p - light;
      d.x *= 0.28;                              // stretch along the brush
      float spec = exp(-dot(d, d) * 7.0);
      float band = exp(-pow((p.y - light.y) * 2.6, 2.0));   // wide horizontal sheen

      // ── 4. diffraction fringes ────────────────────────────────────────
      // Phase is a function of position and viewing angle; a little grain jitter
      // makes the rainbow look etched rather than printed.
      float phase1 = p.y * 1.8 + p.x * 0.35 + roll * 0.6 - pitch * 0.9 + grain * 0.25;
      float phase2 = p.y * 4.5 - p.x * 0.8 - roll * 1.0 + pitch * 0.5 + grain * 0.12;
      float3 rainbow1 = spectrum(phase1);
      float3 rainbow2 = spectrum(phase2);

      // Rainbow shows only where the light band sweeps; elsewhere the metal stays gray
      // with a faint hue shift so it never looks like flat paint.
      float3 foil = col * 0.65 + rainbow1 * 0.35;
      col = mix(col, foil, band * 0.7 + spec * 0.1);
      col += (rainbow2 - 0.5) * (0.02 + band * 0.06);

      // ── 5. glitter facets ─────────────────────────────────────────────
      float2 cellUv = p * 170.0;
      float2 cell = floor(cellUv);
      float h = hash21(cell);
      // jittered round facet inside each cell
      float2 centre = float2(hash21(cell + 3.1), hash21(cell + 9.7)) * 0.5 + 0.25;
      float facet = smoothstep(0.32, 0.05, length(fract(cellUv) - centre));
      float facetAngle = h * 2.0 * PI;
      float align = cos(facetAngle + roll * 7.0 + pitch * 5.5);
      float sparkle = facet * smoothstep(0.96, 1.0, align) * step(0.8, hash21(cell + 7.31));
      col += sparkle * (0.5 + 0.5 * spectrum(h + roll * 0.3)) * (0.4 + band * 0.6) * 0.5;

      // ── 6. plastic glare ──────────────────────────────────────────────
      col += spec * 0.16;
      float2 g = p - light;
      float glare = exp(-dot(g, g) * 1.4) * 0.06;
      col += glare;

      // metal edge darkening
      float edge = smoothstep(0.0, 0.06, uv.x) * smoothstep(0.0, 0.06, 1.0 - uv.x)
                 * smoothstep(0.0, 0.14, uv.y) * smoothstep(0.0, 0.14, 1.0 - uv.y);
      col *= 0.82 + 0.18 * edge;

      return half4(half3(clamp(col, 0.0, 1.0)), 1.0);
  }
  """
    .trimIndent()
