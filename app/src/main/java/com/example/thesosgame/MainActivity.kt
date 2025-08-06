package com.example.thesosgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.thesosgame.data.GameConfig
import com.example.thesosgame.ui.screens.GameScreen
import com.example.thesosgame.ui.screens.OnboardingScreen
import com.example.thesosgame.ui.theme.TheSOSgameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TheSOSgameTheme {
                SOSGameApp()
            }
        }
    }
}

@Composable
fun SOSGameApp() {
    var currentScreen by remember { mutableStateOf(Screen.Onboarding) }
    var gameConfig by remember { mutableStateOf<GameConfig?>(null) }
    
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when (currentScreen) {
            Screen.Onboarding -> {
                OnboardingScreen(
                    onStartGame = { config ->
                        gameConfig = config
                        currentScreen = Screen.Game
                    }
                )
            }
            
            Screen.Game -> {
                gameConfig?.let { config ->
                    GameScreen(
                        gameConfig = config,
                        onBackToOnboarding = {
                            currentScreen = Screen.Onboarding
                        }
                    )
                }
            }
        }
    }
}

enum class Screen {
    Onboarding,
    Game
}