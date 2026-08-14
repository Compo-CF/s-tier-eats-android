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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.compofelice.stiereats.data.CommunityTier
import com.compofelice.stiereats.ui.AppViewModel
import com.compofelice.stiereats.ui.BoardSource
import com.compofelice.stiereats.ui.TierBadge

@Composable
fun CommunityScreen(
    vm: AppViewModel,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var source by remember { mutableStateOf(BoardSource.EVERYONE) }
    var board by remember { mutableStateOf<Map<String, CommunityTier>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(source) {
        loading = true
        board = vm.communityBoard(source)
        loading = false
    }

    // Rank by average desc, then vote count desc.
    val ranked = remember(board, vm.restaurantsById) {
        board.entries
            .mapNotNull { (id, ct) -> vm.restaurantsById[id]?.let { Triple(id, it, ct) } }
            .sortedWith(compareByDescending<Triple<String, com.compofelice.stiereats.data.Restaurant, CommunityTier>> { it.third.average }
                .thenByDescending { it.third.count })
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BoardSource.entries.forEach { s ->
                FilterChip(
                    selected = source == s,
                    onClick = { source = s },
                    label = { Text(s.label) },
                )
            }
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            ranked.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (source == BoardSource.FRIENDS) "Follow people to see their board."
                    else "No rankings yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(ranked, key = { it.first }) { (id, r, ct) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TierBadge(ct.tier, size = 36)
                        Column(Modifier.weight(1f)) {
                            Text(r.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${ct.count} ${if (ct.count == 1) "vote" else "votes"} · avg ${"%.1f".format(ct.average)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
