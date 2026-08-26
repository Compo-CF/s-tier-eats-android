package com.compofelice.stiereats.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.compofelice.stiereats.data.FoodieRank
import com.compofelice.stiereats.data.Tier
import com.compofelice.stiereats.ui.AppViewModel
import com.compofelice.stiereats.ui.TierBadge
import com.compofelice.stiereats.ui.tierColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyStatsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val ranked = vm.myPlacements.size
    val visited = vm.visited.size
    val rank = FoodieRank.from(ranked)
    // Count per tier for the breakdown bar.
    val perTier = Tier.entries.associateWith { t -> vm.myPlacements.values.count { it == t } }
    val maxCount = (perTier.values.maxOrNull() ?: 0).coerceAtLeast(1)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Stats") },
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
            // Top counters
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Ranked", ranked.toString(), Modifier.weight(1f))
                StatTile("Visited", visited.toString(), Modifier.weight(1f))
            }

            if (rank != null) {
                Spacer(Modifier.height(20.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Your rank", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(rank.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(rank.blurb, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Your tiers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Tier.entries.forEach { t ->
                val count = perTier[t] ?: 0
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TierBadge(t, size = 34)
                    Spacer(Modifier.width(12.dp))
                    // Proportional bar
                    Box(
                        Modifier
                            .weight(1f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        if (count > 0) {
                            Box(
                                Modifier
                                    .fillMaxWidth((count.toFloat() / maxCount).coerceIn(0.02f, 1f))
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(tierColor(t)),
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(count.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
