package com.compofelice.stiereats.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.compofelice.stiereats.data.Tier
import com.compofelice.stiereats.ui.TierBadge

private data class TierDef(val tier: Tier, val label: String, val meaning: String)

private val defs = listOf(
    TierDef(Tier.S, "S — the best", "Drop-everything great. The spots you send friends to without hesitation."),
    TierDef(Tier.A, "A — excellent", "Consistently really good. A reliable go-to you're always happy with."),
    TierDef(Tier.B, "B — solid", "Good and dependable. Worth a visit, no complaints."),
    TierDef(Tier.C, "C — okay", "Fine in a pinch, but nothing you'd go out of your way for."),
    TierDef(Tier.F, "F — skip it", "Didn't deliver. You wouldn't go back."),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TierGuideScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tier guide") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(24.dp),
        ) {
            Text(
                "How the tiers work",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Instead of squashing every place into a 3.9-to-4.4 star blur, you sort " +
                    "restaurants into five clear tiers. Higher is better.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            defs.forEach { d ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TierBadge(d.tier, size = 48)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(d.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            d.meaning,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider()
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Your list vs. the community",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Your tiers are yours. On the Community tab you'll see the consensus tier — " +
                    "the average of everyone's placements — so you can compare your take with " +
                    "the crowd's, or just with the Foodie Pros.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
