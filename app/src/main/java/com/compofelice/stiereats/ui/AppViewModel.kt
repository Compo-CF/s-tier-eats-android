package com.compofelice.stiereats.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.compofelice.stiereats.auth.AuthManager
import com.compofelice.stiereats.data.CatalogRepository
import com.compofelice.stiereats.data.CommunityTier
import com.compofelice.stiereats.data.DietaryTag
import com.compofelice.stiereats.data.FirestoreRepository
import com.compofelice.stiereats.data.Restaurant
import com.compofelice.stiereats.data.Tier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Which community board to show. */
enum class BoardSource(val label: String) {
    EVERYONE("Everyone"), PROS("Foodie Pros"), FRIENDS("Friends")
}

/**
 * Single shared state holder for the whole app. Bridges the Compose UI to
 * AuthManager + FirestoreRepository + the bundled catalog. All writes optimistic
 * (update local state immediately, fire the Firestore write in the background).
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val auth = AuthManager()
    private val repo = FirestoreRepository()
    private val catalog = CatalogRepository(app)

    var restaurants by mutableStateOf<List<Restaurant>>(emptyList()); private set
    var restaurantsById by mutableStateOf<Map<String, Restaurant>>(emptyMap()); private set
    var myPlacements by mutableStateOf<Map<String, Tier>>(emptyMap()); private set
    var visited by mutableStateOf<Set<String>>(emptySet()); private set

    var isSignedIn by mutableStateOf(auth.isSignedIn); private set
    var displayName by mutableStateOf(auth.displayName); private set

    var catalogLoading by mutableStateOf(true); private set

    fun bootstrap() {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                val bundled = catalog.loadBundled()
                val live = repo.liveRestaurants()
                (bundled + live).associateBy { it.id }.values.toList()
            }
            restaurants = loaded.sortedBy { it.name.lowercase() }
            restaurantsById = loaded.associateBy { it.id }
            catalogLoading = false
            refreshUserData()
        }
    }

    private fun refreshUserData() {
        if (!auth.isSignedIn) return
        viewModelScope.launch {
            myPlacements = repo.myPlacements()
            visited = repo.visitedList()
        }
    }

    // ── Auth ─────────────────────────────────────────────────────
    suspend fun signIn(context: Context): String? = try {
        auth.signInWithGoogle(context)
        isSignedIn = true
        displayName = auth.displayName
        refreshUserData()
        auth.displayName?.let { repo.saveProfile(it, status = null) }
        null
    } catch (e: Exception) {
        e.message ?: "Sign-in failed"
    }

    fun signOut() {
        auth.signOut()
        isSignedIn = false
        displayName = null
        myPlacements = emptyMap()
        visited = emptySet()
    }

    // ── Writes (optimistic) ──────────────────────────────────────
    fun place(restaurantId: String, tier: Tier) {
        myPlacements = myPlacements + (restaurantId to tier)
        viewModelScope.launch { repo.savePlacement(restaurantId, tier) }
    }

    fun removePlacement(restaurantId: String) {
        myPlacements = myPlacements - restaurantId
        viewModelScope.launch { repo.removePlacement(restaurantId) }
    }

    fun toggleVisited(restaurantId: String) {
        visited = if (restaurantId in visited) visited - restaurantId else visited + restaurantId
        viewModelScope.launch { repo.saveVisitedList(visited) }
    }

    fun setDietary(restaurantId: String, tag: DietaryTag, on: Boolean) {
        viewModelScope.launch { repo.setDietaryTag(restaurantId, tag.rawValue, on) }
    }

    fun saveDisplayName(name: String) {
        displayName = name
        viewModelScope.launch { repo.saveProfile(name, status = null) }
    }

    // ── Reads passed through to the screens ──────────────────────
    suspend fun communityBoard(source: BoardSource): Map<String, CommunityTier> = when (source) {
        BoardSource.EVERYONE -> repo.allCommunityTiers()
        BoardSource.PROS -> repo.proCommunityTiers()
        BoardSource.FRIENDS -> repo.friendsCommunityTiers(emptySet()) // friends UI TODO
    }

    suspend fun communityTier(restaurantId: String): CommunityTier? =
        repo.communityTier(restaurantId)

    suspend fun dietaryTags(restaurantId: String): Pair<Map<String, Int>, Set<String>> =
        repo.dietaryTags(restaurantId)
}
