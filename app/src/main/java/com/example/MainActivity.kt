package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.adaptivesleep.ui.AdaptiveSleepScreen
import com.example.adaptivesleep.ui.AdaptiveSleepViewModel
import com.example.ui.theme.AdaptiveSleepTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AdaptiveSleepViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AdaptiveSleepTheme {
                val uiState by viewModel.uiState.collectAsState()

                // Keep screen on when nightstand session is active or dimmed mode is on
                LaunchedEffect(uiState.isPlaying, uiState.isDimmedMode) {
                    if (uiState.isPlaying || uiState.isDimmedMode) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF08090C)
                ) {
                    AdaptiveSleepScreen(viewModel = viewModel)
                }
            }
        }
    }
}
