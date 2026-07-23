package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.model.UserRole
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel

enum class AppScreen {
    SPLASH,
    ONBOARDING,
    LOGIN_REGISTER,
    PROFILE_WIZARD,
    MAIN_APP
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    // Harmless comment to force rebuild and wake up emulator stream
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.repository.FutbolcuBulRepository.appContext = applicationContext
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentScreen) {
                        AppScreen.SPLASH -> {
                            SplashScreen(onTimeout = {
                                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                if (currentUser != null) {
                                    // Recover session: fetch role from Firestore and initialize real-time sync listeners
                                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    db.collection("users").document(currentUser.uid).get()
                                        .addOnSuccessListener { document ->
                                            if (document.exists()) {
                                                val roleStr = document.getString("role") ?: "PLAYER"
                                                val role = UserRole.valueOf(roleStr)
                                                viewModel.setRole(role)
                                                viewModel.startFirebaseSync()
                                                currentScreen = AppScreen.MAIN_APP
                                            } else {
                                                // If document does not exist, logout and go to onboarding or login
                                                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                                                val onboardingCompleted = getSharedPreferences("app_prefs", MODE_PRIVATE).getBoolean("onboardingCompleted", false)
                                                currentScreen = if (onboardingCompleted) AppScreen.LOGIN_REGISTER else AppScreen.ONBOARDING
                                            }
                                        }
                                        .addOnFailureListener {
                                            // Offline/connection fallback: start sync and guess default role or go to onboarding
                                            viewModel.startFirebaseSync()
                                            currentScreen = AppScreen.LOGIN_REGISTER
                                        }
                                } else {
                                    val onboardingCompleted = getSharedPreferences("app_prefs", MODE_PRIVATE).getBoolean("onboardingCompleted", false)
                                    currentScreen = if (onboardingCompleted) AppScreen.LOGIN_REGISTER else AppScreen.ONBOARDING
                                }
                            })
                        }
                        AppScreen.ONBOARDING -> {
                            OnboardingScreen(onFinished = {
                                getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putBoolean("onboardingCompleted", true).apply()
                                currentScreen = AppScreen.LOGIN_REGISTER
                            })
                        }
                        AppScreen.LOGIN_REGISTER -> {
                            LoginRegisterScreen(
                                viewModel = viewModel,
                                onLoginSuccess = { role ->
                                    currentScreen = AppScreen.MAIN_APP
                                },
                                onRegisterSuccess = { role ->
                                    currentScreen = AppScreen.PROFILE_WIZARD
                                }
                            )
                        }
                        AppScreen.PROFILE_WIZARD -> {
                            ProfileWizardScreen(
                                viewModel = viewModel,
                                onComplete = {
                                    currentScreen = AppScreen.MAIN_APP
                                }
                            )
                        }
                        AppScreen.MAIN_APP -> {
                            MainAppContainer(
                                viewModel = viewModel,
                                onLogout = {
                                    viewModel.logout()
                                    currentScreen = AppScreen.LOGIN_REGISTER
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
