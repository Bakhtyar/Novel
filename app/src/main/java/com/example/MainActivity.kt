package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.screens.CanvasScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.StoryViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: StoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDark = viewModel.isDarkMode
            val isArabic = viewModel.language == "ar"
            var currentScreen by remember { mutableStateOf("home") } // "home", "settings", "canvas"

            val colorScheme = if (isDark) {
                darkColorScheme()
            } else {
                lightColorScheme()
            }

            val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MaterialTheme(colorScheme = colorScheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val projectId = viewModel.currentProjectId

                        when {
                            currentScreen == "settings" -> {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onBack = { currentScreen = if (projectId != null) "canvas" else "home" }
                                )
                            }
                            projectId != null -> {
                                CanvasScreen(
                                    viewModel = viewModel,
                                    onBack = {
                                        viewModel.closeProject()
                                        currentScreen = "home"
                                    }
                                )
                            }
                            else -> {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToSettings = { currentScreen = "settings" }
                                )
                            }
                        }

                        // Watch project opening to switch screen to canvas automatically
                        LaunchedEffect(projectId) {
                            if (projectId != null && currentScreen == "home") {
                                currentScreen = "canvas"
                            }
                        }
                    }
                }
            }
        }
    }
}
