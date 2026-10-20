package com.compofelice.stiereats.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.compofelice.stiereats.ui.AppViewModel
import com.compofelice.stiereats.ui.RestaurantListSkeleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.roundToInt

private class LeaderEntry(
    val uid: String,
    val displayName: String,
    val overlap: Int,
    val agree: Int,
) {
    val matchPct: Int = if (overlap == 0) 0 else (agree * 100.0 / overlap).roundToInt()
}

/**
 * Ranks everyone you follow by how closely their tiers agree with yours.
 * Android twin of iOS TasteMatchLeaderboardView. Friends with no overlapping
 * rankings sink to the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasteMatchLeaderboardScreen(
    vm: AppViewModel,
    onCompare: (String) -> Unit,
    onBack: () -> Unit,
) {
    val friends = vm.friends
    val mine = vm.myPlacements
    var entries by remember { mutableStateOf<List<LeaderEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(friends, mine) {
        loading = true
        val scored = coroutineScope {
            friends.map { f ->
                async {
                    val theirs = vm.placementsOf(f.uid)
                    var overlap = 0
                    var agree = 0
                    for ((rid, t) in theirs) {
                        val m = mine[rid] ?: continue
                        overlap++
                        if (m == t) agree++
                    }
                    LeaderEntry(f.uid, f.displayName, overlap, agree)
                }
            }.awaitAll()
        }
        entries = scored.sortedWith(
            compareByDescending<LeaderEntry> { it.overlap > 0 }
                .thenByDescending { it.matchPct }
                .thenByDescending { it.overlap }
                .thenBy { it.displayName.lowercase() }
        )
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Taste Match") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        when {
            loading -> RestaurantListSkeleton(rows = 6, modifier = Modifier.padding(padding))
            friends.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Follow people to see who shares your taste. Open a friend's shared tier link and tap Follow.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                itemsIndexed(entries, key = { _, e -> e.uid }) { index, e ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable(enabled = e.overlap > 0) { onCompare(e.uid) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RankBadge(index + 1, e.overlap > 0)
                        Column(Modifier.weight(1f)) {
                            Text(e.displayName.ifBlank { "Foodie" }, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                            Text(
                                if (e.overlap > 0) "${e.overlap} shared · agree on ${e.agree}" else "No overlap yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (e.overlap > 0) {
                            Text("${e.matchPct}%", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun RankBadge(rank: Int, matched: Boolean) {
    val medal: Color? = when {
        rank == 1 && matched -> Color(0xFFFFC53D)
        rank == 2 && matched -> Color(0xFFB8BCC4)
        rank == 3 && matched -> Color(0xFFCD7F32)
        else -> null
    }
    Box(
        Modifier.size(36.dp).clip(CircleShape)
            .background(medal?.copy(alpha = 0.25f) ?: MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$rank",
            fontWeight = FontWeight.Bold,
            color = if (matched) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
