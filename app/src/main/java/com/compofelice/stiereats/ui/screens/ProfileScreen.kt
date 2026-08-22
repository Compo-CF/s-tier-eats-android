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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.compofelice.stiereats.data.ProStatus
import com.compofelice.stiereats.ui.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    vm: AppViewModel,
    onOpenAdmin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (vm.isSignedIn) SignedInProfile(vm, onOpenAdmin, modifier)
    else SignedOutProfile(vm, modifier)
}

@Composable
private fun SignedOutProfile(vm: AppViewModel, modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
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
    }
}

@Composable
private fun SignedInProfile(vm: AppViewModel, onOpenAdmin: () -> Unit, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(vm.displayName ?: "") }
    var savedName by remember { mutableStateOf(vm.displayName ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

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
                onClick = { vm.requestFoodiePro() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Request Foodie Pro") }
        }

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
