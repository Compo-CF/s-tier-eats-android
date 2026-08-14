package com.compofelice.stiereats.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.compofelice.stiereats.data.CommunityTier
import com.compofelice.stiereats.data.DietaryTag
import com.compofelice.stiereats.data.Tier
import com.compofelice.stiereats.ui.AppViewModel
import com.compofelice.stiereats.ui.TierBadge
import com.compofelice.stiereats.ui.tierColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    restaurantId: String,
    vm: AppViewModel,
    onBack: () -> Unit,
) {
    val r = vm.restaurantsById[restaurantId]
    var community by remember { mutableStateOf<CommunityTier?>(null) }
    var dietaryCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var myDietary by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(restaurantId) {
        community = vm.communityTier(restaurantId)
        val (counts, mine) = vm.dietaryTags(restaurantId)
        dietaryCounts = counts; myDietary = mine
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(r?.name ?: "Restaurant", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (r == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Restaurant not found")
            }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            val sub = listOfNotNull(
                r.cuisines.firstOrNull()?.replaceFirstChar { it.uppercase() },
                r.area.replaceFirstChar { it.uppercase() }.ifEmpty { null },
                r.priceTier,
            ).joinToString(" · ")
            Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (r.address.isNotBlank()) {
                Text(r.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))

            // Your tier
            Text("Your tier", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            if (!vm.isSignedIn) {
                Text(
                    "Sign in (Profile tab) to rank this spot.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val myTier = vm.myPlacements[restaurantId]
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Tier.entries.forEach { t ->
                        val selected = myTier == t
                        Box(
                            Modifier.size(if (selected) 52.dp else 44.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.material3.Surface(
                                onClick = {
                                    if (selected) vm.removePlacement(restaurantId)
                                    else vm.place(restaurantId, t)
                                },
                                color = tierColor(t),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        t.rawValue,
                                        color = androidx.compose.ui.graphics.Color.White,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            // Community consensus
            Text("Community", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            val c = community
            if (c == null) {
                Text("No community ranking yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TierBadge(c.tier, size = 44)
                    Text(
                        "${c.count} ${if (c.count == 1) "person" else "people"} · avg ${"%.1f".format(c.average)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            // Dietary & needs
            Text("Dietary & needs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DietaryTag.entries.forEach { tag ->
                    val count = dietaryCounts[tag.rawValue] ?: 0
                    val mine = tag.rawValue in myDietary
                    val label = if (count > 0) "${tag.displayName} $count" else tag.displayName
                    if (vm.isSignedIn) {
                        FilterChip(
                            selected = mine,
                            onClick = {
                                val newOn = !mine
                                myDietary = if (newOn) myDietary + tag.rawValue else myDietary - tag.rawValue
                                dietaryCounts = dietaryCounts.toMutableMap().apply {
                                    this[tag.rawValue] = (count + if (newOn) 1 else -1).coerceAtLeast(0)
                                }
                                vm.setDietary(restaurantId, tag, newOn)
                            },
                            label = { Text(label) },
                            leadingIcon = if (mine) { { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) } } else null,
                        )
                    } else {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(label) },
                            colors = AssistChipDefaults.assistChipColors(),
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
