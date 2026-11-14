package com.example.holoscratch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.holoscratch.theme.HoloScratchTheme
import com.example.holoscratch.ui.CouponScreen
import com.example.holoscratch.ui.DemoScreen
import com.example.holoscratch.ui.GravityScreen
import com.example.holoscratch.ui.StartMenu

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { HoloScratchTheme { DemoApp() } }
  }
}

/**
 * Plain state-based navigation: the start menu opens one demo screen at a time,
 * and system back returns to the menu.
 */
@Composable
private fun DemoApp() {
  var screen by rememberSaveable { mutableStateOf(DemoScreen.Menu) }

  BackHandler(enabled = screen != DemoScreen.Menu) { screen = DemoScreen.Menu }

  when (screen) {
    DemoScreen.Menu -> StartMenu(onOpen = { screen = it })
    DemoScreen.Scratch -> CouponScreen()
    DemoScreen.Gravity -> GravityScreen()
  }
}
