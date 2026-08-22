package com.compofelice.stiereats

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.compofelice.stiereats.ui.AppNav
import com.compofelice.stiereats.ui.AppViewModel
import com.compofelice.stiereats.ui.OnboardingScreen
import com.compofelice.stiereats.ui.theme.STierEatsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            STierEatsTheme {
                val prefs = remember {
                    getSharedPreferences("stier_prefs", Context.MODE_PRIVATE)
                }
                var onboarded by remember {
                    mutableStateOf(prefs.getBoolean("onboarded", false))
                }
                if (!onboarded) {
                    OnboardingScreen(onDone = {
                        prefs.edit().putBoolean("onboarded", true).apply()
                        onboarded = true
                    })
                } else {
                    val vm: AppViewModel = viewModel()
                    LaunchedEffect(Unit) { vm.bootstrap() }
                    AppNav(vm)
                }
            }
        }
    }
}
