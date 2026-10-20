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
import com.compofelice.stiereats.data.Friend
import com.compofelice.stiereats.data.GeoPoint
import com.compofelice.stiereats.data.ProStatus
import com.compofelice.stiereats.data.Restaurant
import com.compofelice.stiereats.data.RestaurantList
import com.compofelice.stiereats.data.Tier
import com.compofelice.stiereats.location.LocationProvider
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
    private val locationProvider = LocationProvider(app)

    var restaurants by mutableStateOf<List<Restaurant>>(emptyList()); private set
    var restaurantsById by mutableStateOf<Map<String, Restaurant>>(emptyMap()); private set
    var myPlacements by mutableStateOf<Map<String, Tier>>(emptyMap()); private set
    var visited by mutableStateOf<Set<String>>(emptySet()); private set

    /** People the signed-in user follows (the Android-internal follow graph). */
    var friends by mutableStateOf<List<Friend>>(emptyList()); private set
    val friendIds: Set<String> get() = friends.map { it.uid }.toSet()
    fun isFollowing(uid: String): Boolean = friends.any { it.uid == uid }

    /** Last known device location, or null until permission is granted + a fix
     *  is obtained. Drives Near Me and Plan a Night Out "near me" mode. */
    var userLocation by mutableStateOf<GeoPoint?>(null); private set

    var isSignedIn by mutableStateOf(auth.isSignedIn); private set
    var displayName by mutableStateOf(auth.displayName); private set
    var proStatus by mutableStateOf(ProStatus.NONE); private set
    var isAdmin by mutableStateOf(false); private set

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
            friends = repo.myFriends()
            proStatus = repo.myProStatus()
            isAdmin = repo.isAdmin()
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
        friends = emptyList()
        proStatus = ProStatus.NONE
        isAdmin = false
    }

    fun requestFoodiePro() {
        proStatus = ProStatus.REQUESTED
        viewModelScope.launch { repo.requestFoodiePro() }
    }

    /** The signed-in user's Firebase UID (their account id), or null. */
    fun accountId(): String? = repo.currentUid()

    // ── Friends / follow graph ───────────────────────────────────
    fun follow(targetUid: String, displayName: String) {
        viewModelScope.launch { friends = repo.follow(targetUid, displayName) }
    }

    fun unfollow(targetUid: String) {
        viewModelScope.launch { friends = repo.unfollow(targetUid) }
    }

    /** A specific user's placements — for a friend's tier list, Taste Match and
     *  the leaderboard. */
    suspend fun placementsOf(uid: String): Map<String, Tier> = repo.placements(uid)

    /** A user's display name (from their profile), or null. */
    suspend fun profileName(uid: String): String? = repo.profileName(uid)

    // ── Lists (shareable collections) ────────────────────────────
    suspend fun myLists(): List<RestaurantList> = repo.fetchMyLists()
    suspend fun fetchList(id: String): RestaurantList? = repo.fetchList(id)
    suspend fun createList(title: String, emoji: String = "", isPublic: Boolean = true): String? =
        repo.createList(title, emoji, isPublic)
    suspend fun updateList(
        id: String,
        title: String? = null,
        emoji: String? = null,
        restaurantIDs: List<String>? = null,
        isPublic: Boolean? = null,
    ) = repo.updateList(id, title, emoji, restaurantIDs, isPublic)
    suspend fun deleteList(id: String) = repo.deleteList(id)

    /** Fetch the current device location. Caller MUST have confirmed the
     *  COARSE_LOCATION runtime permission first; on any failure leaves
     *  [userLocation] unchanged (screens fall back to "search by area"). */
    suspend fun refreshLocation() {
        locationProvider.current()?.let { userLocation = it }
    }

    /** Submit a "missing restaurant" suggestion for admin review. */
    suspend fun submitSuggestion(
        name: String,
        address: String,
        area: String,
        cuisines: List<String>,
        description: String,
    ): String? = repo.submitSuggestion(name, address, area, cuisines, description)

    // ── Admin ────────────────────────────────────────────────────
    suspend fun proLists() = repo.fetchProLists()
    suspend fun adminStats() = repo.adminStats()

    fun approvePro(userID: String, onDone: () -> Unit) {
        viewModelScope.launch { repo.approvePro(userID); onDone() }
    }

    fun revokePro(userID: String, onDone: () -> Unit) {
        viewModelScope.launch { repo.revokePro(userID); onDone() }
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

    /** Permanently delete the user's account + data (Play requirement). */
    suspend fun deleteAccount() {
        repo.deleteAccount()
        isSignedIn = false
        displayName = null
        myPlacements = emptyMap()
        visited = emptySet()
    }

    // ── Reads passed through to the screens ──────────────────────
    suspend fun communityBoard(source: BoardSource): Map<String, CommunityTier> = when (source) {
        BoardSource.EVERYONE -> repo.allCommunityTiers()
        BoardSource.PROS -> repo.proCommunityTiers()
        BoardSource.FRIENDS -> repo.friendsCommunityTiers(friendIds)
    }

    suspend fun communityTier(restaurantId: String): CommunityTier? =
        repo.communityTier(restaurantId)

    suspend fun dietaryTags(restaurantId: String): Pair<Map<String, Int>, Set<String>> =
        repo.dietaryTags(restaurantId)
}
