package com.compofelice.stiereats.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The shareable "Food Wrapped" stats card — the Android twin of iOS
 * `StatsShareCard`. Rank pill + four stat tiles + most-explored area. Swap
 * [headline] to "2026 in Food" for the year-end seasonal push.
 */
@Composable
fun StatsShareCard(
    displayName: String?,
    headline: String,
    rankName: String,
    ranked: Int,
    visited: Int,
    sTier: Int,
    topCuisine: String?,
    topArea: String?,
    modifier: Modifier = Modifier,
) {
    val cardBg = Color(0xFF14121C)
    val brandPurple = Color(0xFF6B5CF6)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(cardBg)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("S-TIER EATS", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 3.sp)
            Text(headline, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 30.sp, textAlign = TextAlign.Center)
            if (!displayName.isNullOrBlank()) {
                Text(displayName, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }

        Text(
            rankName,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(brandPurple)
                .padding(horizontal = 20.dp, vertical = 8.dp),
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(ranked.toString(), "Ranked", Modifier.weight(1f))
            StatTile(sTier.toString(), "S-Tier picks", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(visited.toString(), "Visited", Modifier.weight(1f))
            StatTile(topCuisine ?: "—", "Top cuisine", Modifier.weight(1f))
        }

        if (!topArea.isNullOrBlank()) {
            Text("Most-explored area: $topArea", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, textAlign = TextAlign.Center)
        }

        Text("Rank your local restaurants · on Google Play", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(vertical = 18.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 30.sp, maxLines = 1, textAlign = TextAlign.Center)
        Text(label, color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}
