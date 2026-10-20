package com.compofelice.stiereats.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.compofelice.stiereats.data.Restaurant
import com.compofelice.stiereats.data.Tier
import com.compofelice.stiereats.ui.tierColor

/**
 * The shareable tier-list card (dark/purple brand look) — the Android twin of
 * iOS `ShareableTierCard`. Rendered inside a preview sheet and captured to a
 * PNG by [captureToLayer] / [shareCapturedCard]. Each tier row caps at 3 lines
 * so a large list can't make the card unboundedly tall.
 */
@Composable
fun TierShareCard(
    grouped: Map<Tier, List<Restaurant>>,
    displayName: String?,
    modifier: Modifier = Modifier,
) {
    val cardBg = Color(0xFF14121C)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(cardBg)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "S-TIER EATS",
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 3.sp,
            )
            Text(
                if (displayName.isNullOrBlank()) "My Tier List" else "$displayName's Tier List",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
            )
        }

        Tier.entries.forEach { tier ->
            val items = grouped[tier].orEmpty()
            if (items.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(tierColor(tier)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(tier.rawValue, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                    Text(
                        items.joinToString("   ·   ") { it.name },
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 15.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Text(
            "Rank your local restaurants · on Google Play",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
        )
    }
}
