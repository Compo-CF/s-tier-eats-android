package com.compofelice.stiereats.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
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
import com.compofelice.stiereats.ui.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))

        if (vm.isSignedIn) {
            var name by remember { mutableStateOf(vm.displayName ?: "") }
            // Tracks the last-persisted name so the button can show clear
            // "Saved ✓" feedback (the write itself is silent/optimistic).
            var savedName by remember { mutableStateOf(vm.displayName ?: "") }
            Text(
                "Signed in",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${vm.myPlacements.size} ranked · ${vm.visited.size} visited",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                onClick = {
                    val trimmed = name.trim()
                    vm.saveDisplayName(trimmed)
                    savedName = trimmed
                },
                enabled = name.isNotBlank() && !isSaved,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (isSaved) "Saved ✓" else "Save name") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { vm.signOut() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Sign out") }
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Delete account", color = MaterialTheme.colorScheme.error) }

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
                                    deleting = true
                                    vm.deleteAccount()
                                    deleting = false
                                    confirmDelete = false
                                }
                            },
                        ) { Text(if (deleting) "Deleting…" else "Delete", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(enabled = !deleting, onClick = { confirmDelete = false }) { Text("Cancel") }
                    },
                )
            }
        } else {
            Text(
                "Sign in to rank restaurants",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
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
                onClick = {
                    scope.launch {
                        busy = true
                        error = vm.signIn(context)
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (busy) "Signing in…" else "Sign in with Google") }
            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
        }
    }
}
