package com.compofelice.stiereats.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.compofelice.stiereats.data.CommunityTier
import com.compofelice.stiereats.data.METERS_PER_MILE
import com.compofelice.stiereats.data.Restaurant
import com.compofelice.stiereats.data.Tier
import com.compofelice.stiereats.data.distanceMetersTo
import com.compofelice.stiereats.data.milesString
import com.compofelice.stiereats.ui.AppViewModel
import com.compofelice.stiereats.ui.BoardSource
import com.compofelice.stiereats.ui.tierColor
import kotlinx.coroutines.launch

/** Which rankings to draw tonight's pick from. Friends is intentionally omitted
 *  until the friends feature ships (no half-built option). */
private enum class NightOutSource(val label: String) {
    MY_TIERS("My Tiers"), COMMUNITY("Community"), PROS("Foodie Pros")
}

/** Minimum acceptable tier — a floor, not an exact match. */
private enum class TierFloor(val label: String, val minScore: Int) {
    S_ONLY("S", 5), SA("S–A", 4), SB("S–B", 3), ANY("Any", 1)
}

private val RADIUS_OPTIONS = listOf(2.0, 5.0, 10.0, 25.0)
private val PRICE_OPTIONS = listOf("$", "$$", "$$$", "$$$$")

private data class Pick(
    val restaurant: Restaurant,
    val tier: Tier,
    val miles: Double?,
    val rankCount: Int?,
)

/**
 * "Plan a Night Out" — a decision engine over the tier data. Pick a source,
 * cuisine, location (near me + radius, or by area), quality floor and price,
 * then get a single "Tonight's Pick" with alternates and Spin again. Mirrors
 * iOS NightOutView. Pure UX over data already loaded — no new backend.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NightOutScreen(
    vm: AppViewModel,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted; if (granted) scope.launch { vm.refreshLocation() } }
    LaunchedEffect(Unit) { if (hasPermission) vm.refreshLocation() }

    // Inputs
    var source by remember { mutableStateOf(NightOutSource.MY_TIERS) }
    var useNearMe by remember { mutableStateOf(true) }
    var radiusMiles by remember { mutableStateOf(10.0) }
    var floor by remember { mutableStateOf(TierFloor.SB) }
    val cuisines = remember { mutableStateOf(setOf<String>()) }
    val prices = remember { mutableStateOf(setOf<String>()) }

    val allAreas = remember(vm.restaurants) {
        vm.restaurants.map { it.area }.filter { it.isNotBlank() }.distinct().sorted()
    }
    var selectedArea by remember(allAreas) {
        mutableStateOf(allAreas.firstOrNull { it.contains("wood", true) } ?: allAreas.firstOrNull() ?: "")
    }
    var areaMenuOpen by remember { mutableStateOf(false) }

    val topCuisines = remember(vm.restaurants) {
        vm.restaurants.flatMap { it.cuisines }.groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }.map { it.key }.take(28)
    }

    // Runtime
    var searching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<Pick>>(emptyList()) }
    var spinIndex by remember { mutableStateOf(0) }

    fun runFind() {
        scope.launch {
            searching = true
            searchError = null
            spinIndex = 0
            if (source == NightOutSource.MY_TIERS && vm.myPlacements.isEmpty()) {
                results = emptyList(); hasSearched = true; searching = false
                searchError = "You haven't ranked any spots yet. Rank a few, or switch to Community."
                return@launch
            }
            if (useNearMe && vm.userLocation == null) {
                results = emptyList(); hasSearched = true; searching = false
                searchError = "Location is off. Turn it on for “near me”, or switch to “By area”."
                return@launch
            }
            val sourceTiers: Map<String, CommunityTier> = when (source) {
                NightOutSource.MY_TIERS -> emptyMap()
                NightOutSource.COMMUNITY -> vm.communityBoard(BoardSource.EVERYONE)
                NightOutSource.PROS -> vm.communityBoard(BoardSource.PROS)
            }
            results = makeRanked(
                vm = vm,
                source = source,
                sourceTiers = sourceTiers,
                cuisines = cuisines.value,
                prices = prices.value,
                floor = floor,
                useNearMe = useNearMe,
                radiusMiles = radiusMiles,
                selectedArea = selectedArea,
            )
            hasSearched = true
            searching = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plan a Night Out") },
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
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text("Tonight's the night", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Tell us the vibe — we'll pick the spot.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            SectionLabel("Whose rankings")
            SingleChipRow(
                options = NightOutSource.entries.map { it to it.label },
                selected = source,
                onSelect = { source = it },
            )
            Spacer(Modifier.height(16.dp))

            SectionLabel("Cuisine (optional)")
            MultiChipRow(
                options = topCuisines.map { it to it.replaceFirstChar { c -> c.uppercase() } },
                selected = cuisines.value,
                onToggle = { c ->
                    cuisines.value = if (c in cuisines.value) cuisines.value - c else cuisines.value + c
                },
            )
            Spacer(Modifier.height(16.dp))

            SectionLabel("Where")
            SingleChipRow(
                options = listOf(true to "Near me", false to "By area"),
                selected = useNearMe,
                onSelect = { useNearMe = it },
            )
            Spacer(Modifier.height(8.dp))
            if (useNearMe) {
                if (vm.userLocation == null) {
                    Text(
                        "Location is off — enable it or search by area.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(onClick = {
                        permLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }) { Text("Enable location") }
                    Spacer(Modifier.height(8.dp))
                }
                SingleChipRow(
                    options = RADIUS_OPTIONS.map { it to "${it.toInt()} mi" },
                    selected = radiusMiles,
                    onSelect = { radiusMiles = it },
                )
            } else {
                Box {
                    OutlinedButton(onClick = { areaMenuOpen = true }) {
                        Text(selectedArea.replaceFirstChar { it.uppercase() }.ifBlank { "Choose area" })
                    }
                    DropdownMenu(expanded = areaMenuOpen, onDismissRequest = { areaMenuOpen = false }) {
                        allAreas.forEach { a ->
                            DropdownMenuItem(
                                text = { Text(a.replaceFirstChar { it.uppercase() }) },
                                onClick = { selectedArea = a; areaMenuOpen = false },
                                leadingIcon = if (a == selectedArea) { { Icon(Icons.Filled.Check, null) } } else null,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            SectionLabel("How good")
            SingleChipRow(
                options = TierFloor.entries.map { it to it.label },
                selected = floor,
                onSelect = { floor = it },
            )
            Spacer(Modifier.height(16.dp))

            SectionLabel("Price (optional)")
            MultiChipRow(
                options = PRICE_OPTIONS.map { it to it },
                selected = prices.value,
                onToggle = { p ->
                    prices.value = if (p in prices.value) prices.value - p else prices.value + p
                },
            )
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { runFind() },
                enabled = !searching,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (searching) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Find Tonight's Pick")
                }
            }
            Spacer(Modifier.height(20.dp))

            val err = searchError
            if (err != null && !searching) {
                Text(err, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            } else if (hasSearched && results.isEmpty() && !searching) {
                Text(
                    "Nothing matched all your filters. Widen the radius, lower the quality floor, or open up the cuisine/price.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (results.isNotEmpty() && !searching) {
                ResultBlock(
                    results = results,
                    spinIndex = spinIndex,
                    onSpin = { spinIndex += 1 },
                    onOpen = onOpen,
                    onDirections = { r -> openDirections(context, r) },
                )
            }
        }
    }
}

/** The matcher: filter by every input, score (tier dominates, then proximity,
 *  then crowd confidence), sort best-first. Mirrors iOS NightOutView.makeRanked. */
private fun makeRanked(
    vm: AppViewModel,
    source: NightOutSource,
    sourceTiers: Map<String, CommunityTier>,
    cuisines: Set<String>,
    prices: Set<String>,
    floor: TierFloor,
    useNearMe: Boolean,
    radiusMiles: Double,
    selectedArea: String,
): List<Pick> {
    val loc = vm.userLocation
    val scored = mutableListOf<Pair<Pick, Double>>()

    fun consider(r: Restaurant, tier: Tier, count: Int?) {
        if (tier.score < floor.minScore) return
        if (cuisines.isNotEmpty() && cuisines.none { it in r.cuisines }) return
        if (prices.isNotEmpty() && r.priceTier !in prices) return

        var miles: Double? = null
        if (useNearMe) {
            if (loc == null) return
            val m = r.distanceMetersTo(loc) / METERS_PER_MILE
            if (m > radiusMiles) return
            miles = m
        } else {
            if (r.area != selectedArea) return
        }

        var score = tier.score * 100.0
        miles?.let { score += ((radiusMiles - it) / radiusMiles).coerceAtLeast(0.0) * 25 }
        count?.let { score += minOf(it, 20) }
        scored.add(Pick(r, tier, miles, count) to score)
    }

    if (source == NightOutSource.MY_TIERS) {
        for ((rid, tier) in vm.myPlacements) {
            vm.restaurantsById[rid]?.let { consider(it, tier, null) }
        }
    } else {
        for ((rid, info) in sourceTiers) {
            vm.restaurantsById[rid]?.let { consider(it, info.tier, info.count) }
        }
    }
    return scored.sortedByDescending { it.second }.map { it.first }
}

@Composable
private fun ResultBlock(
    results: List<Pick>,
    spinIndex: Int,
    onSpin: () -> Unit,
    onOpen: (String) -> Unit,
    onDirections: (Restaurant) -> Unit,
) {
    val hero = results[spinIndex % results.size]
    val alternates = results.filter { it.restaurant.id != hero.restaurant.id }.take(3)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    ) {
        Surface(
            color = tierColor(hero.tier).copy(alpha = 0.12f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(color = tierColor(hero.tier), shape = RoundedCornerShape(18.dp), modifier = Modifier.size(72.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(hero.tier.rawValue, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(hero.restaurant.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                val meta = listOfNotNull(
                    hero.restaurant.cuisines.firstOrNull()?.replaceFirstChar { it.uppercase() },
                    hero.restaurant.priceTier.ifBlank { null },
                    hero.miles?.let { milesString(it * METERS_PER_MILE) }
                        ?: hero.restaurant.area.replaceFirstChar { it.uppercase() }.ifBlank { null },
                    hero.rankCount?.let { "$it ranked" },
                ).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { onOpen(hero.restaurant.id) }, modifier = Modifier.weight(1f)) {
                        Text("See details")
                    }
                    Button(onClick = { onDirections(hero.restaurant) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Directions, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Directions")
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        if (results.size > 1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onSpin) {
                    Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Spin again")
                }
            }
        }

        if (alternates.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Or maybe…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            alternates.forEach { pick ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(pick.restaurant.id) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(color = tierColor(pick.tier), shape = RoundedCornerShape(9.dp), modifier = Modifier.size(38.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(pick.tier.rawValue, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(pick.restaurant.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(
                            pick.restaurant.cuisines.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    pick.miles?.let {
                        Text(milesString(it * METERS_PER_MILE), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
}

/** A single-select row of FilterChips (horizontally scrollable). */
@Composable
private fun <T> SingleChipRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label) },
            )
        }
    }
}

/** A multi-select row of FilterChips (horizontally scrollable). */
@Composable
private fun MultiChipRow(
    options: List<Pair<String, String>>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value in selected,
                onClick = { onToggle(value) },
                label = { Text(label) },
                leadingIcon = if (value in selected) { { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) } } else null,
            )
        }
    }
}

private fun openDirections(context: Context, r: Restaurant) {
    try {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${r.latitude},${r.longitude}"),
            ),
        )
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No maps app available", Toast.LENGTH_SHORT).show()
    }
}
