package com.compofelice.stiereats.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.compofelice.stiereats.ui.theme.TierA
import com.compofelice.stiereats.ui.theme.TierB
import com.compofelice.stiereats.ui.theme.TierC
import com.compofelice.stiereats.ui.theme.TierF
import com.compofelice.stiereats.ui.theme.TierS

/**
 * v1.4: brief animated launch splash. The five tier badges rise/fade in one
 * after another over a dark brand gradient, then MainActivity cross-fades it
 * out. Pure presentation — no data work. Mirrors the iOS SplashView.
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    val tiers = listOf("S" to TierS, "A" to TierA, "B" to TierB, "C" to TierC, "F" to TierF)
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A1824), Color(0xFF0C0B12)))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                tiers.forEachIndexed { i, (letter, color) ->
                    val t by animateFloatAsState(
                        targetValue = if (started) 1f else 0f,
                        animationSpec = tween(durationMillis = 430, delayMillis = i * 85),
                        label = "badge$i",
                    )
                    Box(
                        Modifier
                            .size(60.dp)
                            .scale(0.5f + 0.5f * t)
                            .alpha(t)
                            .background(color, RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(letter, color = Color.White, fontWeight = FontWeight.Black, fontSize = 30.sp)
                    }
                }
            }
            val wordmark by animateFloatAsState(
                targetValue = if (started) 1f else 0f,
                animationSpec = tween(durationMillis = 500, delayMillis = 460),
                label = "wordmark",
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "S-Tier Eats",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 30.sp,
                modifier = Modifier.alpha(wordmark),
            )
        }
    }
}
