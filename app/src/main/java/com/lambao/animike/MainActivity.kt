package com.lambao.animike

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lambao.animike.ui.home.HomeScreen
import com.lambao.animike.ui.theme.AniMikeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // AniMike là dark-only (v1) — force icon status/nav bar sáng thay vì
        // để enableEdgeToEdge() suy ra từ chế độ dark mode của hệ thống.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            AniMikeTheme {
                HomeScreen()
            }
        }
    }
}
