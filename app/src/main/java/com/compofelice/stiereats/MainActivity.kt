package com.compofelice.stiereats

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.compofelice.stiereats.ui.AppNav
import com.compofelice.stiereats.ui.AppViewModel
import com.compofelice.stiereats.ui.OnboardingScreen
import com.compofelice.stiereats.ui.SplashScreen
import com.compofelice.stiereats.ui.theme.STierEatsTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    /** Pending friend uid parsed from a `tier/<uid>` deep link (custom scheme
     *  `stier://tier/<uid>` or the https share link). Observed in composition;
     *  AppNav navigates to that friend and clears it. */
    private val deepLinkUid = mutableStateOf<String?>(null)
    private val deepLinkListId = mutableStateOf<String?>(null)

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        val segs = data.pathSegments ?: return
        fun after(key: String): String? {
            val i = segs.indexOf(key)
            return if (i >= 0 && i + 1 < segs.size) segs[i + 1] else null
        }
        when {
            data.scheme == "stier" && data.host == "tier" -> segs.firstOrNull()?.let { if (it.isNotBlank()) deepLinkUid.value = it }
            data.scheme == "stier" && data.host == "list" -> segs.firstOrNull()?.let { if (it.isNotBlank()) deepLinkListId.value = it }
            data.scheme == "https" || data.scheme == "http" -> {
                after("tier")?.let { if (it.isNotBlank()) deepLinkUid.value = it }
                after("list")?.let { if (it.isNotBlank()) deepLinkListId.value = it }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleDeepLink(intent)
        setContent {
            STierEatsTheme {
                // v1.4: hold the animated splash for a short beat, then fade it
                // out to reveal the app.
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(1500)
                    showSplash = false
                }
                Box(Modifier.fillMaxSize()) {
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
                        AppNav(
                            vm,
                            deepLinkFriendUid = deepLinkUid.value,
                            deepLinkListId = deepLinkListId.value,
                            onConsumeDeepLink = { deepLinkUid.value = null; deepLinkListId.value = null },
                        )
                    }
                    AnimatedVisibility(
                        visible = showSplash,
                        enter = EnterTransition.None,
                        exit = fadeOut(tween(450)),
                    ) {
                        SplashScreen()
                    }
                }
            }
        }
    }
}
