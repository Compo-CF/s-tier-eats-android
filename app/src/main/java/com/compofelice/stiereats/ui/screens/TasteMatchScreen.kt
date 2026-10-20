package com.compofelice.stiereats.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.compofelice.stiereats.data.Restaurant
import com.compofelice.stiereats.data.Tier
import com.compofelice.stiereats.ui.AppViewModel
import com.compofelice.stiereats.ui.tierColor
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 1:1 "Taste Match" — compares your tiers to a friend's over the spots you've
 * both ranked, ordered by biggest disagreement. Android twin of iOS
 * FriendCompareView.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasteMatchScreen(
    friendUid: String,
    vm: AppViewModel,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
) {
    var name by remember { mutableStateOf<String?>(null) }
    var theirs by remember { mutableStateOf<Map<String, Tier>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(friendUid) {
        name = vm.profileName(friendUid)
        theirs = vm.placementsOf(friendUid)
        loading = false
    }

    val displayName = name?.takeIf { it.isNotBlank() } ?: "your friend"
    val mine = vm.myPlacements

    val rows = remember(theirs, mine, vm.restaurantsById) {
        mine.mapNotNull { (rid, m) ->
            val t = theirs[rid] ?: return@mapNotNull null
            val r = vm.restaurantsById[rid] ?: return@mapNotNull null
            MatchRow(r, m, t)
        }.sortedWith(
            compareByDescending<MatchRow> { it.gap }.thenBy { it.restaurant.name.lowercase() }
        )
    }
    val agree = rows.count { it.agree }
    val pct = if (rows.isEmpty()) 0 else (agree * 100.0 / rows.size).roundToInt()

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
            loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            rows.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "You and $displayName haven't ranked any of the same restaurants yet. Rank a few more and check back.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("$pct%", fontWeight = FontWeight.Bold, fontSize = 48.sp, color = MaterialTheme.colorScheme.primary)
                        Text("taste match with $displayName", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Both ranked ${rows.size} · agree on $agree",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider()
                }
                items(rows, key = { it.restaurant.id }) { row ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpen(row.restaurant.id) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(row.restaurant.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MiniTier("You", row.mine)
                                MiniTier(displayName, row.theirs)
                            }
                        }
                        Text(
                            if (row.agree) "✓" else "↔",
                            color = if (row.agree) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

private class MatchRow(val restaurant: Restaurant, val mine: Tier, val theirs: Tier) {
    val gap: Int = abs(mine.score - theirs.score)
    val agree: Boolean = mine == theirs
}

@Composable
private fun MiniTier(label: String, tier: Tier) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Box(
            Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(tierColor(tier)),
            contentAlignment = Alignment.Center,
        ) {
            Text(tier.rawValue, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}
