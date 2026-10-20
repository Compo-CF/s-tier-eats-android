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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.compofelice.stiereats.ui.AppViewModel

/**
 * The Friends hub: the people you follow, with a Taste Match leaderboard entry.
 * Following happens by opening a friend's shared tier link (deep link) → their
 * tier screen → Follow. Share your own link from Profile.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    vm: AppViewModel,
    onOpenFriend: (String) -> Unit,
    onOpenLeaderboard: () -> Unit,
    onBack: () -> Unit,
) {
    val friends = vm.friends

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    if (friends.isNotEmpty()) {
                        IconButton(onClick = onOpenLeaderboard) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = "Taste match leaderboard")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (friends.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "You're not following anyone yet.\n\nOpen a friend's shared tier link and tap Follow — or share your own list from Profile so friends can follow you.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                Button(
                    onClick = onOpenLeaderboard,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Icon(Icons.Filled.EmojiEvents, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Taste Match leaderboard")
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(friends, key = { it.uid }) { f ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpenFriend(f.uid) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                f.displayName.ifBlank { "Foodie" },
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
