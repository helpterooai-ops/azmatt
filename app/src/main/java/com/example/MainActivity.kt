package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.GlassBottomNavigation
import com.example.ui.components.NavTab
import com.example.ui.screens.CreateReportScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthViewModel

enum class AppFlowState {
    REGISTER,
    LOGIN,
    MAIN_APP
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AzamatAppShell()
                }
            }
        }
    }
}

@Composable
fun AzamatAppShell(
    authViewModel: AuthViewModel = viewModel()
) {
    val isUserLoggedIn by authViewModel.isUserLoggedIn.collectAsState()

    // Default to LOGIN screen if not logged in
    val initialFlow = if (isUserLoggedIn) AppFlowState.MAIN_APP else AppFlowState.LOGIN
    var flowState by remember(isUserLoggedIn) { mutableStateOf(initialFlow) }
    var selectedTab by remember { mutableStateOf(NavTab.HOME) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (flowState) {
            AppFlowState.LOGIN -> {
                LoginScreen(
                    authViewModel = authViewModel,
                    onNavigateToRegister = { flowState = AppFlowState.REGISTER },
                    onNavigateToHome = { flowState = AppFlowState.MAIN_APP },
                    modifier = Modifier.statusBarsPadding()
                )
            }
            AppFlowState.REGISTER -> {
                RegisterScreen(
                    authViewModel = authViewModel,
                    onNavigateToLogin = { flowState = AppFlowState.LOGIN },
                    modifier = Modifier.statusBarsPadding()
                )
            }
            AppFlowState.MAIN_APP -> {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        GlassBottomNavigation(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "TabTransition"
                        ) { targetTab ->
                            when (targetTab) {
                                NavTab.HOME -> HomeScreen()
                                NavTab.CREATE_REPORT -> CreateReportScreen()
                                NavTab.NOTIFICATIONS -> NotificationsScreen()
                                NavTab.PROFILE -> ProfileScreen(
                                    authViewModel = authViewModel,
                                    onNavigateToLogin = { flowState = AppFlowState.LOGIN }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
