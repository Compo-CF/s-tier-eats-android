package com.compofelice.stiereats.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    var community by remember { mutableStateOf<CommunityTier?>(null) }
    var dietaryCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var myDietary by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showOrderPicker by remember { mutableStateOf(false) }

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

            // Quick actions — call / directions / website / order / reserve, the
            // "act on this place" row iOS has (RestaurantDetailView + DeliveryPickerSheet).
            val phone = r.phone
            val website = r.website
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = {
                        launchUri(
                            context,
                            "https://www.google.com/maps/dir/?api=1&destination=${r.latitude},${r.longitude}",
                        )
                    },
                    label = { Text("Directions") },
                    leadingIcon = { Icon(Icons.Filled.Directions, null, Modifier.size(18.dp)) },
                )
                if (!phone.isNullOrBlank()) {
                    AssistChip(
                        onClick = { launchDial(context, phone) },
                        label = { Text("Call") },
                        leadingIcon = { Icon(Icons.Filled.Call, null, Modifier.size(18.dp)) },
                    )
                }
                if (!website.isNullOrBlank()) {
                    AssistChip(
                        onClick = { launchUri(context, website) },
                        label = { Text("Website") },
                        leadingIcon = { Icon(Icons.Filled.Language, null, Modifier.size(18.dp)) },
                    )
                }
                AssistChip(
                    onClick = { showOrderPicker = true },
                    label = { Text("Order") },
                    leadingIcon = { Icon(Icons.Filled.ShoppingBag, null, Modifier.size(18.dp)) },
                )
                // Reserve only shows where the spot actually takes reservations
                // (curated takesReservations flag). The OpenTable search term
                // carries the city so it resolves to the right venue; no
                // affiliate tag yet (plain link, affiliate-ready later).
                if (r.reservable) {
                    val city = r.address.substringBefore(", TX").substringAfterLast(", ")
                        .ifBlank { r.area }
                    AssistChip(
                        onClick = {
                            launchUri(context, "https://www.opentable.com/s?term=${Uri.encode("${r.name} $city TX")}")
                        },
                        label = { Text("Reserve") },
                        leadingIcon = { Icon(Icons.Filled.EventSeat, null, Modifier.size(18.dp)) },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            if (showOrderPicker) {
                AlertDialog(
                    onDismissRequest = { showOrderPicker = false },
                    title = { Text("Order from…") },
                    text = { Text("Open a delivery search for ${r.name}.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showOrderPicker = false
                            launchUri(
                                context,
                                "https://www.doordash.com/search/store/${Uri.encode(r.name + " " + r.area)}",
                            )
                        }) { Text("DoorDash") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showOrderPicker = false
                            launchUri(context, "https://www.ubereats.com/search?q=${Uri.encode(r.name)}")
                        }) { Text("Uber Eats") }
                    },
                )
            }

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
                            Modifier.size(if (selected) 54.dp else 44.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.material3.Surface(
                                onClick = {
                                    if (selected) {
                                        vm.removePlacement(restaurantId)
                                        Toast.makeText(context, "Removed from your tiers", Toast.LENGTH_SHORT).show()
                                    } else {
                                        vm.place(restaurantId, t)
                                        Toast.makeText(context, "Saved to your ${t.rawValue} tier", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                color = tierColor(t),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                // S-01: make the chosen tier unmistakable — a white ring
                                // (the size bump alone was too subtle next to the chips).
                                border = if (selected) BorderStroke(3.dp, androidx.compose.ui.graphics.Color.White) else null,
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
                if (myTier != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Your pick: ${myTier.rawValue}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = tierColor(myTier),
                    )
                }

                // M-01: mark visited — the Profile/My Stats "Visited" stat had no
                // control anywhere, so it always read 0. This wires it up.
                Spacer(Modifier.height(14.dp))
                val isVisited = restaurantId in vm.visited
                FilterChip(
                    selected = isVisited,
                    onClick = {
                        vm.toggleVisited(restaurantId)
                        Toast.makeText(
                            context,
                            if (isVisited) "Removed from visited" else "Marked as visited",
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    label = { Text(if (isVisited) "Visited" else "Mark visited") },
                    leadingIcon = if (isVisited) { { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) } } else null,
                )
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

/** Open a URL in the browser / handling app, with a graceful Toast if nothing
 *  can handle it. */
private fun launchUri(context: Context, uri: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app available to open that", Toast.LENGTH_SHORT).show()
    }
}

/** Open the phone dialer pre-filled with a number (ACTION_DIAL needs no permission). */
private fun launchDial(context: Context, phone: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No dialer available", Toast.LENGTH_SHORT).show()
    }
}
