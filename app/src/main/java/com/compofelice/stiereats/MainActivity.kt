package com.compofelice.stiereats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.compofelice.stiereats.auth.AuthManager
import com.compofelice.stiereats.ui.theme.STierEatsTheme
import kotlinx.coroutines.launch

/**
 * Temporary shell. Confirms the Google Sign-In → Firebase path works, then the
 * real tabbed app (Map / Browse / My Tiers / Community / Profile) gets built on
 * top of a NavHost. This screen is a placeholder to prove the auth + Firestore
 * foundation before the screens land.
 */
class MainActivity : ComponentActivity() {
    private val auth = AuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            STierEatsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    SignInGate(
                        auth = auth,
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
    }
}

@Composable
private fun SignInGate(auth: AuthManager, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(if (auth.isSignedIn) "Signed in as ${auth.displayName ?: auth.uid}" else "Not signed in") }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("S-Tier Eats", style = MaterialTheme.typography.headlineMedium)
        Text(status, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = {
            scope.launch {
                status = try {
                    auth.signInWithGoogle(context)
                    "Signed in as ${auth.displayName ?: auth.uid}"
                } catch (e: Exception) {
                    "Sign-in failed: ${e.message}"
                }
            }
        }) { Text("Sign in with Google") }
    }
}
