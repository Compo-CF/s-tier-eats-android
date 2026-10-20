package com.compofelice.stiereats.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.compofelice.stiereats.data.Tier
import com.compofelice.stiereats.ui.AppViewModel
import com.compofelice.stiereats.ui.TierBadge

/**
 * Read-only view of a user's tier list, reached from a shared `tier/<uid>`
 * link (deep-linked) or from the Friends hub. Follow/unfollow + a Taste Match
 * entry. Android twin of iOS FriendTierView.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendTierScreen(
    friendUid: String,
    vm: AppViewModel,
    onOpen: (String) -> Unit,
    onCompare: (String) -> Unit,
    onBack: () -> Unit,
) {
    var name by remember { mutableStateOf<String?>(null) }
    var placements by remember { mutableStateOf<Map<String, Tier>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(friendUid) {
        name = vm.profileName(friendUid)
        placements = vm.placementsOf(friendUid)
        loading = false
    }

    val isMe = vm.accountId() == friendUid
    val following = vm.isFollowing(friendUid)
    val title = name?.takeIf { it.isNotBlank() }?.let { "$it's Tiers" } ?: "Tier list"

    val grouped = remember(placements, vm.restaurantsById) {
        Tier.entries.associateWith { t ->
            placements.filter { it.value == t }.keys
                .mapNotNull { vm.restaurantsById[it] }
                .sortedBy { it.name.lowercase() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    if (placements.isNotEmpty()) {
                        IconButton(onClick = { onCompare(friendUid) }) {
                            Icon(Icons.AutoMirrored.Filled.CompareArrows, "Taste match")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            placements.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No rankings yet, or this link is no longer valid.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            ) {
                if (!isMe) {
                    if (following) {
                        OutlinedButton(onClick = { vm.unfollow(friendUid) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Following ✓")
                        }
                    } else {
                        Button(onClick = { vm.follow(friendUid, name ?: "Foodie") }, modifier = Modifier.fillMaxWidth()) {
                            Text("Follow")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Tier.entries.forEach { tier ->
                    val items = grouped[tier].orEmpty()
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        TierBadge(tier, size = 48)
                        Spacer(Modifier.width(10.dp))
                        if (items.isEmpty()) {
                            Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
                        } else {
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items.forEach { r ->
                                    Card(onClick = { onOpen(r.id) }, shape = RoundedCornerShape(10.dp)) {
                                        Column(Modifier.padding(8.dp).width(120.dp)) {
                                            Text(r.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                            r.cuisines.firstOrNull()?.let {
                                                Text(it.replaceFirstChar { c -> c.uppercase() }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
