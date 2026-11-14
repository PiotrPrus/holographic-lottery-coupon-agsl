package dev.piotrprus.holoscratch.presentation.slides

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.Canvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.piotrprus.holoscratch.presentation.deck.Slide
import dev.piotrprus.holoscratch.presentation.deck.SlideScope
import dev.piotrprus.holoscratch.presentation.demos.MouseTilt
import dev.piotrprus.holoscratch.presentation.demos.TiltArea
import dev.piotrprus.holoscratch.presentation.demos.holoFoilBrush
import dev.piotrprus.holoscratch.presentation.demos.rememberMouseTilt
import dev.piotrprus.holoscratch.presentation.ui.DeckColors
import dev.piotrprus.holoscratch.presentation.ui.LabeledFrame
import dev.piotrprus.holoscratch.presentation.ui.MorphingCodeBlock
import dev.piotrprus.holoscratch.presentation.ui.SlideSubtitle
import dev.piotrprus.holoscratch.presentation.ui.SlideSurface
import dev.piotrprus.holoscratch.presentation.ui.SlideTitle
import dev.piotrprus.holoscratch.presentation.ui.VideoClip

val Danger = Color(0xFFFF6B6B)
val Good = Color(0xFF5BE49B)

val StageBackground = Brush.verticalGradient(listOf(Color(0xFF0A0D12), Color(0xFF131A2B)))

/**
 * Content slide with live tilt: the whole stage is the tilt pad, so moving the mouse anywhere
 * drives the foil. Title = the API name, subtitle = what it means in plain words.
 */
@Composable
fun LiveSlide(
    title: String? = null,
    subtitle: String? = null,
    content: @Composable ColumnScope.(MouseTilt) -> Unit,
) {
    val tilt = rememberMouseTilt()
    TiltArea(tilt, Modifier.fillMaxSize()) {
        SlideSurface {
            if (title != null) {
                Crossfade(targetState = title to subtitle, animationSpec = tween(500)) { (t, s) ->
                    Column {
                        SlideTitle(t)
                        if (s != null) SlideSubtitle(s)
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
            content(tilt)
        }
    }
}

/**
 * The BYCCTL "build" layout: one snippet growing step by step on the left (new lines slide in
 * highlighted), the live result of exactly that code on the right.
 */
@Composable
fun SlideScope.BuildSlide(
    title: String,
    subtitle: String?,
    code: String,
    codeWidth: Dp = 1080.dp,
    codeFontSize: TextUnit = 24.sp,
    previewLabel: String = "Preview",
    previewSize: Pair<Dp, Dp> = 560.dp to 380.dp,
    highlight: Set<Int>? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
    below: @Composable ColumnScope.(MouseTilt) -> Unit = {},
    preview: @Composable BoxScope.(MouseTilt) -> Unit,
) {
    LiveSlide(title, subtitle) { tilt ->
        Row(horizontalArrangement = Arrangement.spacedBy(50.dp)) {
            Box(Modifier.width(codeWidth).height(700.dp)) {
                MorphingCodeBlock(
                    code = code,
                    fontSize = codeFontSize,
                    highlight = highlight,
                    modifier = Modifier.fillMaxSize(),
                )
                overlay()
            }
            Column {
                LabeledFrame(
                    label = previewLabel,
                    frameModifier = Modifier.width(previewSize.first).height(previewSize.second),
                ) {
                    Box(Modifier.fillMaxSize().background(PreviewBackground).padding(28.dp)) {
                        preview(tilt)
                    }
                }
                Spacer(Modifier.height(30.dp))
                below(tilt)
            }
        }
    }
}

val PreviewBackground = Color(0xFF1B2130)

/** Docs-style definition box: the API name in mono, then the KDoc / docs sentence. */
@Composable
fun KDocCallout(term: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color(0xFF1C222C), RoundedCornerShape(14.dp))
            .border(1.dp, DeckColors.codeBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 32.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(term, color = DeckColors.textSecondary, fontFamily = FontFamily.Monospace, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text(text, color = DeckColors.textPrimary, fontSize = 26.sp, lineHeight = 38.sp)
    }
}

/** KDoc box that lands on top of the code, after the code has finished scrolling. */
@Composable
fun BoxScope.KDocOverlay(visible: Boolean, term: String, text: String) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
        enter = fadeIn(tween(500, delayMillis = 700)) + slideInVertically(tween(500, delayMillis = 700)) { it / 8 },
        exit = fadeOut(tween(300)),
    ) {
        KDocCallout(term, text, Modifier.fillMaxWidth())
    }
}

/** Big holo-filled text: the foil shader drawn SrcAtop over the glyphs. */
@Composable
fun HoloText(
    text: String,
    tilt: MouseTilt,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    lineHeight: TextUnit = fontSize * 1.15f,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontWeight = FontWeight.Black,
        color = Color.White,
        textAlign = textAlign,
        modifier = modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    drawRect(holoFoilBrush(size, tilt.value), blendMode = BlendMode.SrcAtop)
                }
            },
    )
}

/** Chapter divider: a holo headline and one plain line under it. */
fun chapterSlide(title: String, subtitle: String? = null) = Slide {
    val tilt = rememberMouseTilt()
    TiltArea(tilt, Modifier.fillMaxSize().background(StageBackground)) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HoloText(title, tilt, fontSize = 130.sp, textAlign = TextAlign.Center)
            if (subtitle != null) {
                Spacer(Modifier.height(30.dp))
                Text(subtitle, color = DeckColors.textSecondary, fontSize = 42.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

/** A quiet question in the middle of the stage; optional answer fades in on the next step. */
fun questionSlide(question: String, answer: String? = null) = Slide(steps = if (answer != null) 1 else 0) {
    Box(Modifier.fillMaxSize().background(StageBackground), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(56.dp),
            modifier = Modifier.padding(horizontal = 160.dp),
        ) {
            Text(
                question,
                color = DeckColors.textPrimary,
                fontSize = 88.sp,
                lineHeight = 108.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            if (answer != null) {
                Reveal(at = 1) {
                    Text(
                        answer,
                        style = TextStyle(brush = Brush.horizontalGradient(listOf(DeckColors.accent, DeckColors.accentAlt))),
                        fontSize = 52.sp,
                        lineHeight = 66.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
fun Callout(text: String, modifier: Modifier = Modifier, color: Color = DeckColors.warm, label: String = "TIP") {
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .border(2.dp, color.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .padding(horizontal = 30.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = color, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
        Spacer(Modifier.width(24.dp))
        Text(text, color = DeckColors.textPrimary, fontSize = 30.sp, lineHeight = 40.sp)
    }
}

@Composable
fun DemoLabel(text: String, good: Boolean? = null, modifier: Modifier = Modifier) {
    val color = when (good) {
        true -> Good
        false -> Danger
        null -> DeckColors.textSecondary
    }
    Text(text, color = color, fontSize = 32.sp, fontWeight = FontWeight.SemiBold, modifier = modifier)
}

/** A labelled demo area; [visible] fades the whole thing (label + demo) in and out. */
@Composable
fun LabeledDemo(
    label: String,
    good: Boolean? = null,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    showLabel: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(500))
    val labelAlpha by animateFloatAsState(if (showLabel) 1f else 0f, tween(500))
    Column(modifier = modifier.graphicsLayer { this.alpha = alpha }) {
        DemoLabel(label, good, Modifier.graphicsLayer { this.alpha = labelAlpha })
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth(), content = content)
    }
}

/** Horizontal meter, 0..1. */
@Composable
fun Meter(label: String, value: Float, color: Color, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(value.coerceIn(0f, 1f), tween(120))
    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(label, color = DeckColors.textPrimary, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(
                "%3d%%".format((value * 100).toInt()),
                color = color,
                fontSize = 44.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(DeckColors.surface),
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(animated).background(color))
        }
    }
}

/** Phone-shaped slot for a real-device recording; shows where to drop the file until it exists. */
@Composable
fun DeviceRecording(resourcePath: String, modifier: Modifier = Modifier, height: Dp = 860.dp) {
    LabeledFrame(
        label = "On the phone",
        modifier = modifier,
        frameModifier = Modifier.height(height).aspectRatio(9f / 19.5f),
    ) {
        VideoClip(resourcePath = resourcePath, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
fun Caption(text: String, modifier: Modifier = Modifier, color: Color = DeckColors.textSecondary) {
    Text(text, color = color, fontSize = 32.sp, lineHeight = 44.sp, modifier = modifier)
}

@Composable
fun Gap(size: Dp) = Spacer(Modifier.size(size))

/**
 * Small "reset" chip for live demos. Uses a plain tap detector, not `clickable`: a clickable takes
 * focus, and then Space/Enter would press the button instead of advancing the slide.
 */
@Composable
fun ResetButton(onReset: () -> Unit, modifier: Modifier = Modifier) = ChipButton("↺  reset", onReset, modifier)

/** Same tap-only chip, any label. */
@Composable
fun ChipButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        label,
        color = DeckColors.textSecondary,
        fontSize = 26.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .border(1.dp, DeckColors.codeBorder, RoundedCornerShape(12.dp))
            .pointerInput(onClick) { detectTapGestures { onClick() } }
            .padding(horizontal = 22.dp, vertical = 10.dp),
    )
}

/**
 * A live-demo slider driven by drag or tap only. Material's Slider takes keyboard focus and then
 * eats the arrow keys the deck uses to change slides; this one never takes focus.
 */
@Composable
fun DragSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    format: (Float) -> String = { "%.0f".format(it) },
) {
    fun valueAt(x: Float, width: Int) =
        (range.start + (x / width.coerceAtLeast(1)).coerceIn(0f, 1f) * (range.endInclusive - range.start))
    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(label, color = DeckColors.textSecondary, fontSize = 24.sp, modifier = Modifier.weight(1f))
            Text(format(value), color = DeckColors.accentAlt, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(10.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .pointerInput(range) {
                    detectTapGestures { onChange(valueAt(it.x, size.width)) }
                }
                .pointerInput(range) {
                    detectDragGestures { change, _ -> onChange(valueAt(change.position.x, size.width)) }
                },
        ) {
            val y = size.height / 2f
            val f = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
            drawLine(DeckColors.codeBorder, Offset(0f, y), Offset(size.width, y), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
            drawLine(DeckColors.accentAlt, Offset(0f, y), Offset(size.width * f, y), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(Color.White, radius = 13.dp.toPx(), center = Offset(size.width * f, y))
        }
    }
}
