package com.compofelice.stiereats.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.compofelice.stiereats.data.RestaurantList
import com.compofelice.stiereats.ui.AppViewModel
import com.compofelice.stiereats.ui.share.ListShareCard
import com.compofelice.stiereats.ui.share.ShareCardSheet
import kotlinx.coroutines.launch

/**
 * A list's contents: add/remove/rename, public toggle, share (image + link),
 * delete. Android twin of iOS ListDetailView. [readOnly] (a shared deep link)
 * hides every edit control; edits are also gated on the list being yours.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    listId: String,
    vm: AppViewModel,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    readOnly: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var loaded by remember { mutableStateOf<RestaurantList?>(null) }
    var loading by remember { mutableStateOf(true) }
    var title by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    var ids by remember { mutableStateOf<List<String>>(emptyList()) }

    var showAdd by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var showShare by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(listId) {
        val l = vm.fetchList(listId)
        loaded = l
        if (l != null) {
            title = l.title; emoji = l.emoji; isPublic = l.isPublic; ids = l.restaurantIDs
        }
        loading = false
    }

    fun persist() {
        scope.launch { vm.updateList(listId, title = title, emoji = emoji, restaurantIDs = ids, isPublic = isPublic) }
    }

    val restaurants = remember(ids, vm.restaurantsById) { ids.mapNotNull { vm.restaurantsById[it] } }
    val editable = !readOnly && vm.accountId() != null && loaded?.ownerId == vm.accountId()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title.ifBlank { "List" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    if (restaurants.isNotEmpty()) {
                        IconButton(onClick = { showShare = true }) { Icon(Icons.Filled.Share, "Share as image") }
                    }
                    if (isPublic && restaurants.isNotEmpty()) {
                        IconButton(onClick = {
                            val link = "https://compo-cf.github.io/woodlands-eats/list/$listId"
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "$title — my S-Tier Eats list: $link")
                            }
                            context.startActivity(Intent.createChooser(send, "Share link"))
                        }) { Icon(Icons.Filled.Link, "Share link") }
                    }
                    if (editable) {
                        IconButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, "Add restaurant") }
                        IconButton(onClick = { renameText = title; showRename = true }) { Icon(Icons.Filled.Edit, "Rename") }
                    }
                },
            )
        },
    ) { padding ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            loaded == null -> Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text("This list is private or no longer exists.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            ) {
                if (editable) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Public (shareable)", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = isPublic, onCheckedChange = { isPublic = it; persist() })
                    }
                    HorizontalDivider()
                }

                if (restaurants.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (editable) "Add restaurants with the + button." else "This list is empty.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    restaurants.forEachIndexed { i, r ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpen(r.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("${i + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(28.dp))
                            Column(Modifier.weight(1f)) {
                                Text(r.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                                val sub = listOfNotNull(
                                    r.cuisines.firstOrNull()?.replaceFirstChar { it.uppercase() },
                                    r.area.replaceFirstChar { it.uppercase() }.ifEmpty { null },
                                ).joinToString(" · ")
                                if (sub.isNotEmpty()) {
                                    Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (editable) {
                                IconButton(onClick = { ids = ids.filterNot { it == r.id }; persist() }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }

                if (editable) {
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    ) { Text("Delete list", color = MaterialTheme.colorScheme.error) }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (showAdd) {
        AddToListSheet(
            vm = vm,
            existing = ids.toSet(),
            onDismiss = { showAdd = false },
            onPick = { id ->
                if (id !in ids) { ids = ids + id; persist() }
                showAdd = false
            },
        )
    }

    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rename list") },
            text = {
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    val t = renameText.trim()
                    showRename = false
                    if (t.isNotEmpty()) { title = t; persist() }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete list?") },
            text = { Text("This permanently deletes “$title”. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch { vm.deleteList(listId); onBack() }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }

    if (showShare) {
        ShareCardSheet(
            onDismiss = { showShare = false },
            fileName = "stier-list.png",
            shareText = "$title — my S-Tier Eats list. https://play.google.com/store/apps/details?id=com.compofelice.stiereats",
        ) {
            ListShareCard(title = title, emoji = emoji, restaurants = restaurants)
        }
    }
}
