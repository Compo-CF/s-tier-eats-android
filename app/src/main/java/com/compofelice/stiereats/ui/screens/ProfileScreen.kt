package com.compofelice.stiereats.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.compofelice.stiereats.data.FoodieRank
import com.compofelice.stiereats.data.ProStatus
import com.compofelice.stiereats.ui.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    vm: AppViewModel,
    onOpenAdmin: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSuggest: () -> Unit,
    onOpenTierGuide: () -> Unit,
    onOpenAbout: () -> Unit,
    onReplayIntro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (vm.isSignedIn) {
        SignedInProfile(
            vm, onOpenAdmin, onOpenStats, onOpenSuggest,
            onOpenTierGuide, onOpenAbout, onReplayIntro, modifier,
        )
    } else {
        SignedOutProfile(vm, onOpenTierGuide, onOpenAbout, onReplayIntro, modifier)
    }
}

/** Full name = at least two whitespace-separated parts, each ≥ 2 chars.
 *  Mirrors iOS ProfileView.isFullName so Foodie Pro requests carry a real name. */
private fun isFullName(name: String): Boolean {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return parts.size >= 2 && parts.all { it.length >= 2 }
}

@Composable
private fun SignedOutProfile(
    vm: AppViewModel,
    onOpenTierGuide: () -> Unit,
    onOpenAbout: () -> Unit,
    onReplayIntro: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        Icon(Icons.Filled.AccountCircle, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Sign in to rank restaurants", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            "Your tiers sync across devices and count toward the community boards.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            enabled = !busy,
            onClick = { scope.launch { busy = true; error = vm.signIn(context); busy = false } },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (busy) "Signing in…" else "Sign in with Google") }
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }

        // About menu — available without signing in.
        Spacer(Modifier.height(32.dp))
        MenuRow("Tier guide", Icons.Filled.HelpOutline, onOpenTierGuide)
        MenuRow("Show app tour", Icons.Filled.AutoAwesome, onReplayIntro)
        MenuRow("About S-Tier Eats", Icons.Filled.Info, onOpenAbout)
    }
}

/** A full-width, left-aligned settings row (icon + label). */
@Composable
private fun MenuRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun SignedInProfile(
    vm: AppViewModel,
    onOpenAdmin: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSuggest: () -> Unit,
    onOpenTierGuide: () -> Unit,
    onOpenAbout: () -> Unit,
    onReplayIntro: () -> Unit,
    modifier: Modifier,
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(vm.displayName ?: "") }
    var savedName by remember { mutableStateOf(vm.displayName ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var proError by remember { mutableStateOf<String?>(null) }
    val rank = FoodieRank.from(vm.myPlacements.size)

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Icon(Icons.Filled.AccountCircle, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("Signed in", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "${vm.myPlacements.size} ranked · ${vm.visited.size} visited",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Foodie rank (Newcomer → Tastemaker), matches iOS.
        if (rank != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                "${rank.displayName} · ${rank.blurb}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Foodie Pro standing
        Spacer(Modifier.height(12.dp))
        when (vm.proStatus) {
            ProStatus.APPROVED -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Star, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Foodie Pro", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            ProStatus.REQUESTED -> Text(
                "Foodie Pro — requested, pending review",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ProStatus.NONE -> {}
        }

        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Display name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        val isSaved = name.trim() == savedName && name.isNotBlank()
        Button(
            onClick = { val t = name.trim(); vm.saveDisplayName(t); savedName = t },
            enabled = name.isNotBlank() && !isSaved,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (isSaved) "Saved ✓" else "Save name") }

        if (vm.proStatus == ProStatus.NONE) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    // Foodie Pro needs a real full name so the admin can verify
                    // identity before approval (mirrors iOS).
                    val t = name.trim()
                    when {
                        t.isBlank() ->
                            proError = "Set a display name first — it appears next to your rankings on the Foodie Pro board."
                        !isFullName(t) ->
                            proError = "Foodie Pro requires your full name (first and last) so the admin can verify you."
                        else -> {
                            proError = null
                            if (t != savedName) { vm.saveDisplayName(t); savedName = t }
                            vm.requestFoodiePro()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Request Foodie Pro") }
            proError?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }

        // ── Menu (parity with the iOS Profile) ───────────────────
        Spacer(Modifier.height(20.dp))
        MenuRow("My Stats", Icons.Filled.BarChart, onOpenStats)
        MenuRow("Suggest a missing restaurant", Icons.Filled.AddLocationAlt, onOpenSuggest)
        MenuRow("Tier guide", Icons.Filled.HelpOutline, onOpenTierGuide)
        MenuRow("Show app tour", Icons.Filled.AutoAwesome, onReplayIntro)
        MenuRow("About S-Tier Eats", Icons.Filled.Info, onOpenAbout)

        if (vm.isAdmin) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onOpenAdmin,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Shield, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Admin dashboard")
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { vm.signOut() }, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Delete account", color = MaterialTheme.colorScheme.error)
        }

        // Account id — useful for support / admin bootstrap (iOS shows the iCloud id).
        vm.accountId()?.let { id ->
            Spacer(Modifier.height(16.dp))
            Text("Account ID", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(id, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(24.dp))

        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { if (!deleting) confirmDelete = false },
                title = { Text("Delete account?") },
                text = { Text("This permanently deletes your account and all your rankings, tags, visited list, and profile. This can't be undone.") },
                confirmButton = {
                    TextButton(
                        enabled = !deleting,
                        onClick = {
                            scope.launch {
                                deleting = true; vm.deleteAccount(); deleting = false; confirmDelete = false
                            }
                        },
                    ) { Text(if (deleting) "Deleting…" else "Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(enabled = !deleting, onClick = { confirmDelete = false }) { Text("Cancel") }
                },
            )
        }
    }
}
