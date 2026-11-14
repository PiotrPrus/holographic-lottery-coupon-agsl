package dev.piotrprus.holoscratch.presentation.demos

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeShader
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

/**
 * The holo foil shader from `holo/HoloFoilShader.kt`, running on desktop.
 *
 * AGSL on Android *is* Skia's SkSL, so the source is the same. Desktop uses Skia's
 * RuntimeEffect where Android uses RuntimeShader. Two uniforms exist only for the talk:
 *
 *  uLayers — how many of the layers are switched on (0 = flat grey … 5 = everything), so the
 *            deck can build the material one layer at a time
 *  uOil    — 0 = tuned hue speed, 1 = the first attempt (2.0 / 3.0), which looked like an oil slick
 *
 * One change from the app: `pow(x, 2.0)` is written `x * x`. pow() is undefined for a negative
 * base, and the desktop GPU backends take that literally.
 */
private const val HOLO_FOIL_SKSL = """
uniform float2 uResolution;
uniform float2 uTilt;
uniform float uLayers;
uniform float uOil;

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

    // 1. brushed grain
    float grain = 0.0;
    if (uLayers >= 1.0) {
        grain += vnoise(float2(p.x * 5.0,  p.y * 1100.0)) * 0.50;
        grain += vnoise(float2(p.x * 16.0, p.y * 380.0))  * 0.32;
        grain += vnoise(float2(p.x * 55.0, p.y * 140.0))  * 0.18;
        grain -= 0.5;
    }
    float3 col = float3(0.58, 0.59, 0.62) + grain * 0.11;

    // 2. light position + anisotropic specular
    float2 light = float2(0.5 * aspect + roll * 1.1 * aspect, 0.5 - pitch * 1.05);
    float2 d = p - light;
    d.x *= 0.28;
    float spec = exp(-dot(d, d) * 7.0);
    float by = (p.y - light.y) * 2.6;
    float band = exp(-(by * by));
    if (uLayers >= 2.0) {
        col += spec * 0.16;
    }

    // 3. diffraction fringes
    if (uLayers >= 3.0) {
        float rollGain = mix(0.6, 2.0, uOil);
        float pitchGain = mix(0.9, 3.0, uOil);
        float phase1 = p.y * 1.8 + p.x * 0.35 + roll * rollGain - pitch * pitchGain + grain * 0.25;
        float phase2 = p.y * 4.5 - p.x * 0.8 - roll * 1.0 + pitch * 0.5 + grain * 0.12;
        float3 rainbow1 = spectrum(phase1);
        float3 rainbow2 = spectrum(phase2);
        float3 foil = col * 0.65 + rainbow1 * 0.35;
        col = mix(col, foil, band * 0.7 + spec * 0.1);
        col += (rainbow2 - 0.5) * (0.02 + band * 0.06);
    }

    // 4. glitter facets
    if (uLayers >= 4.0) {
        float2 cellUv = p * 170.0;
        float2 cell = floor(cellUv);
        float h = hash21(cell);
        float2 centre = float2(hash21(cell + 3.1), hash21(cell + 9.7)) * 0.5 + 0.25;
        float facet = smoothstep(0.32, 0.05, length(fract(cellUv) - centre));
        float facetAngle = h * 2.0 * PI;
        float align = cos(facetAngle + roll * 7.0 + pitch * 5.5);
        float sparkle = facet * smoothstep(0.96, 1.0, align) * step(0.8, hash21(cell + 7.31));
        col += sparkle * (0.5 + 0.5 * spectrum(h + roll * 0.3)) * (0.4 + band * 0.6) * 0.5;
    }

    // 5. plastic glare + edge darkening
    if (uLayers >= 5.0) {
        float2 g = p - light;
        col += exp(-dot(g, g) * 1.4) * 0.06;
        float edge = smoothstep(0.0, 0.06, uv.x) * smoothstep(0.0, 0.06, 1.0 - uv.x)
                   * smoothstep(0.0, 0.14, uv.y) * smoothstep(0.0, 0.14, 1.0 - uv.y);
        col *= 0.82 + 0.18 * edge;
    }

    return half4(half3(clamp(col, 0.0, 1.0)), 1.0);
}
"""

const val HOLO_ALL_LAYERS = 5

private val holoEffect: RuntimeEffect by lazy { RuntimeEffect.makeForShader(HOLO_FOIL_SKSL) }

/** Builds a brush for one frame. Cheap: the effect is compiled once, only uniforms change. */
fun holoFoilBrush(
    size: Size,
    tilt: Tilt,
    layers: Int = HOLO_ALL_LAYERS,
    oilSlick: Boolean = false,
): ShaderBrush {
    val builder = RuntimeShaderBuilder(holoEffect)
    builder.uniform("uResolution", size.width, size.height)
    builder.uniform("uTilt", tilt.roll, tilt.pitch)
    builder.uniform("uLayers", layers.toFloat())
    builder.uniform("uOil", if (oilSlick) 1f else 0f)
    return ShaderBrush(builder.makeShader().asComposeShader())
}
