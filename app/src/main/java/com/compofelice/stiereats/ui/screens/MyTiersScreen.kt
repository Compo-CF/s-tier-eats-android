package com.compofelice.stiereats.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.compofelice.stiereats.data.Restaurant
import com.compofelice.stiereats.data.Tier
import com.compofelice.stiereats.ui.AppViewModel
import com.compofelice.stiereats.ui.TierBadge
import com.compofelice.stiereats.ui.share.ShareCardSheet
import com.compofelice.stiereats.ui.share.TierShareCard

@Composable
fun MyTiersScreen(
    vm: AppViewModel,
    onOpen: (String) -> Unit,
    onOpenLists: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (!vm.isSignedIn) {
        Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Sign in (Profile tab) to build your tier list.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    if (vm.myPlacements.isEmpty()) {
        Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No rankings yet. Open a restaurant and pick a tier.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    // Group restaurants by tier, S→F.
    val grouped = remember(vm.myPlacements, vm.restaurantsById) {
        Tier.entries.associateWith { tier ->
            vm.myPlacements.filter { it.value == tier }
                .keys.mapNotNull { vm.restaurantsById[it] }
                .sortedBy { it.name.lowercase() }
        }
    }

    var showShare by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onOpenLists) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("My Lists")
            }
            TextButton(onClick = { showShare = true }) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Share")
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            Tier.entries.forEach { tier ->
                val list = grouped[tier].orEmpty()
                if (list.isNotEmpty()) {
                    item(key = "hdr_${tier.rawValue}") {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            TierBadge(tier, size = 36)
                            Text("${list.size}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(list, key = { it.id }) { r ->
                        RestaurantLine(r, onOpen)
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showShare) {
        ShareCardSheet(
            onDismiss = { showShare = false },
            fileName = "stier-tier-list.png",
        ) {
            TierShareCard(grouped = grouped, displayName = vm.displayName)
        }
    }
}

@Composable
private fun RestaurantLine(r: Restaurant, onOpen: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { onOpen(r.id) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(r.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
        val sub = listOfNotNull(
            r.cuisines.firstOrNull()?.replaceFirstChar { it.uppercase() },
            r.area.replaceFirstChar { it.uppercase() }.ifEmpty { null },
        ).joinToString(" · ")
        if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
