package dev.piotrprus.holoscratch.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Dark rounded frame for videos, screenshots and previews. */
@Composable
fun MediaFrame(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0A0D12))
            .border(1.dp, DeckColors.codeBorder, RoundedCornerShape(24.dp)),
        contentAlignment = contentAlignment,
        content = content,
    )
}

/** [MediaFrame] with a small caption above it — the Keynote "Preview" label. */
@Composable
fun LabeledFrame(
    label: String,
    modifier: Modifier = Modifier,
    frameModifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Text(
            text = label,
            color = DeckColors.textSecondary,
            fontSize = 30.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = 8.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        MediaFrame(modifier = frameModifier, content = content)
    }
}

/** Secondary line under a [SlideTitle], e.g. "in graphicsLayer". */
@Composable
fun SlideSubtitle(text: String) {
    Text(
        text = text,
        color = DeckColors.textSecondary,
        fontSize = 36.sp,
        modifier = Modifier.padding(top = 20.dp),
    )
}
