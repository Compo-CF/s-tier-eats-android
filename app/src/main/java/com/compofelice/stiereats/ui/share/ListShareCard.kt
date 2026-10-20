package com.compofelice.stiereats.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.compofelice.stiereats.data.Restaurant

/**
 * The shareable "Top N" list card — Android twin of iOS `ListShareCard`.
 * Numbered restaurants under the list title. Caps at 15 rows so the card stays
 * a sensible height; any overflow shows a "+N more" line.
 */
@Composable
fun ListShareCard(
    title: String,
    emoji: String,
    restaurants: List<Restaurant>,
    modifier: Modifier = Modifier,
) {
    val cardBg = Color(0xFF14121C)
    val brandPurple = Color(0xFF9B8CFF)
    val shown = restaurants.take(15)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(cardBg)
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("S-TIER EATS", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 3.sp)
        Text(
            "${emoji.ifBlank { "📋" }}  ${title.ifBlank { "My List" }}",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
        )
        shown.forEachIndexed { i, r ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${i + 1}", color = brandPurple, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.width(30.dp))
                Text(r.name, color = Color.White.copy(alpha = 0.92f), fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
        }
        if (restaurants.size > shown.size) {
            Text("+${restaurants.size - shown.size} more", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
        }
        Text("Make your own list · on Google Play", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
    }
}
