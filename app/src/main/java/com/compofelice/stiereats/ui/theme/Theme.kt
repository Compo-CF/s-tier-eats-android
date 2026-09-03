package com.compofelice.stiereats.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// Tier palette — S=red, A=orange, B=yellow, C=green, F=purple (matches iOS).
val TierS = Color(0xFFE23B3B)
val TierA = Color(0xFFEE7A2E)
val TierB = Color(0xFFE0C23A)
val TierC = Color(0xFF4FB477)
val TierF = Color(0xFF8C59C7)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6B5CF6),
    secondary = Color(0xFFEE7A2E),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF9B8CFF),
    secondary = Color(0xFFEE7A2E),
)

@Composable
fun STierEatsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
    ) {
        // Wrap in a Surface so LocalContentColor tracks the theme's
        // onBackground. Without it, default (uncolored) Text falls back to
        // black — which is why every default text was dark-on-dark in dark
        // mode. This one wrap fixes contrast app-wide.
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content,
        )
    }
}
