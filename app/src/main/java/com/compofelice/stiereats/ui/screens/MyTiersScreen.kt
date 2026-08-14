package com.compofelice.stiereats.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.compofelice.stiereats.data.Restaurant
import com.compofelice.stiereats.data.Tier
import com.compofelice.stiereats.ui.AppViewModel
import com.compofelice.stiereats.ui.TierBadge

@Composable
fun MyTiersScreen(
    vm: AppViewModel,
    onOpen: (String) -> Unit,
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

    LazyColumn(modifier.fillMaxSize()) {
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
