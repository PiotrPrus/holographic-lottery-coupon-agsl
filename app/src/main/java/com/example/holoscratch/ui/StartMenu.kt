package com.example.holoscratch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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

/** The screens the start menu can open. */
enum class DemoScreen {
  Menu,
  Scratch,
  Gravity,
}

private val MenuBackground = Color(0xFF101018)
private val ButtonFill = Color(0xFF26243A)
private val ButtonText = Color(0xFFF7F1E4)
private val SubText = Color(0xFF9C98B4)

/**
 * The first screen of the app: one big button per demo.
 * Made for screen recordings, so the buttons are large and the text is short.
 */
@Composable
fun StartMenu(onOpen: (DemoScreen) -> Unit, modifier: Modifier = Modifier) {
  Column(
    modifier =
      modifier.fillMaxSize().background(MenuBackground).safeDrawingPadding().padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("HoloScratch", color = ButtonText, fontSize = 36.sp, fontWeight = FontWeight.Black)
    Spacer(Modifier.height(48.dp))
    MenuButton(
      title = "Scratch the card",
      subtitle = "Holo foil, dust and sound",
      onClick = { onOpen(DemoScreen.Scratch) },
    )
    Spacer(Modifier.height(20.dp))
    MenuButton(
      title = "Gravity sensor",
      subtitle = "Raw values, roll and pitch",
      onClick = { onOpen(DemoScreen.Gravity) },
    )
  }
}

/** One large, full-width menu entry. */
@Composable
private fun MenuButton(title: String, subtitle: String, onClick: () -> Unit) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(ButtonFill)
        .clickable(onClick = onClick)
        .padding(horizontal = 28.dp, vertical = 32.dp)
  ) {
    Text(title, color = ButtonText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, color = SubText, fontSize = 16.sp)
  }
}
