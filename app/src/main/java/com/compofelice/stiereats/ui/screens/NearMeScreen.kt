package com.compofelice.stiereats.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.compofelice.stiereats.data.CommunityTier
import com.compofelice.stiereats.data.Restaurant
import com.compofelice.stiereats.data.Tier
import com.compofelice.stiereats.data.distanceMetersTo
import com.compofelice.stiereats.data.milesString
import com.compofelice.stiereats.ui.AppViewModel
import com.compofelice.stiereats.ui.BoardSource
import com.compofelice.stiereats.ui.TierBadge

private const val MAX_RESULTS = 40

/**
 * "Top-rated near me" — community-ranked spots sorted by distance from the user,
 * filterable by cuisine and by a "great spots only" (B+) floor. Mirrors iOS
 * NearMeView. Needs COARSE location; falls back to a permission prompt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearMeScreen(
    vm: AppViewModel,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    var tiers by remember { mutableStateOf<Map<String, CommunityTier>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var goodOnly by remember { mutableStateOf(true) }
    var cuisineFilter by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasPermission) permLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        tiers = vm.communityBoard(BoardSource.EVERYONE)
        loading = false
    }
    LaunchedEffect(hasPermission) {
        if (hasPermission) vm.refreshLocation()
    }

    val loc = vm.userLocation
    val nearbyBase = remember(tiers, loc, goodOnly, vm.restaurantsById) {
        if (loc == null) emptyList()
        else tiers.mapNotNull { (id, ct) ->
            val r = vm.restaurantsById[id] ?: return@mapNotNull null
            if (goodOnly && ct.tier.score < Tier.B.score) return@mapNotNull null
            Triple(r, ct, r.distanceMetersTo(loc))
        }.sortedBy { it.third }
    }
    val availableCuisines = remember(nearbyBase) {
        nearbyBase.flatMap { it.first.cuisines }
            .groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }.map { it.key }
    }
    val nearby = remember(nearbyBase, cuisineFilter) {
        (if (cuisineFilter == null) nearbyBase
        else nearbyBase.filter { it.first.cuisines.contains(cuisineFilter) })
            .take(MAX_RESULTS)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Near Me") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.FilterList, "Filter by cuisine")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("All cuisines") },
                                onClick = { cuisineFilter = null; menuOpen = false },
                                leadingIcon = if (cuisineFilter == null) {
                                    { Icon(Icons.Filled.Check, null) }
                                } else null,
                            )
                            availableCuisines.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.replaceFirstChar { it.uppercase() }) },
                                    onClick = { cuisineFilter = c; menuOpen = false },
                                    leadingIcon = if (cuisineFilter == c) {
                                        { Icon(Icons.Filled.Check, null) }
                                    } else null,
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = goodOnly,
                    onClick = { goodOnly = !goodOnly },
                    label = { Text("Great spots only") },
                    leadingIcon = if (goodOnly) { { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) } } else null,
                )
            }

            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                loc == null -> EmptyMessage(
                    title = "Location needed",
                    body = "Turn on location access to see the best-rated restaurants near you.",
                ) {
                    Button(onClick = {
                        permLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }) { Text("Enable location") }
                }
                nearby.isEmpty() -> EmptyMessage(
                    title = "Nothing ranked nearby yet",
                    body = if (cuisineFilter == null)
                        "No community-ranked spots close by. Try turning off “Great spots only”."
                    else
                        "No ${cuisineFilter} spots ranked close by. Try another cuisine.",
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(nearby, key = { it.first.id }) { (r, ct, meters) ->
                        NearRow(r, ct, meters, onOpen)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun NearRow(
    r: Restaurant,
    ct: CommunityTier,
    meters: Double,
    onOpen: (String) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onOpen(r.id) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TierBadge(ct.tier, size = 40)
        Column(Modifier.weight(1f)) {
            Text(r.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            val cuisine = r.cuisines.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: ""
            if (cuisine.isNotEmpty()) {
                Text(cuisine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(milesString(meters), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                "${ct.count} ${if (ct.count == 1) "rank" else "ranks"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyMessage(
    title: String,
    body: String,
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}
