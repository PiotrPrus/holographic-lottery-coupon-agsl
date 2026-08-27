package com.example.holoscratch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.holoscratch.theme.HoloScratchTheme
import com.example.holoscratch.ui.CouponScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { HoloScratchTheme { CouponScreen() } }
  }
}
