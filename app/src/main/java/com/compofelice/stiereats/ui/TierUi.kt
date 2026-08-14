package com.compofelice.stiereats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.compofelice.stiereats.data.Tier
import com.compofelice.stiereats.ui.theme.TierA
import com.compofelice.stiereats.ui.theme.TierB
import com.compofelice.stiereats.ui.theme.TierC
import com.compofelice.stiereats.ui.theme.TierF
import com.compofelice.stiereats.ui.theme.TierS

fun tierColor(tier: Tier): Color = when (tier) {
    Tier.S -> TierS
    Tier.A -> TierA
    Tier.B -> TierB
    Tier.C -> TierC
    Tier.F -> TierF
}

/** A colored square badge with the tier letter — the app's signature mark. */
@Composable
fun TierBadge(tier: Tier, size: Int = 40, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(tierColor(tier)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            tier.rawValue,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.45f).sp,
        )
    }
}
