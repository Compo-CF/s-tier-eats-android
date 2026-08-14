package com.compofelice.stiereats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.compofelice.stiereats.ui.AppNav
import com.compofelice.stiereats.ui.AppViewModel
import com.compofelice.stiereats.ui.theme.STierEatsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            STierEatsTheme {
                val vm: AppViewModel = viewModel()
                LaunchedEffect(Unit) { vm.bootstrap() }
                AppNav(vm)
            }
        }
    }
}
