package com.compofelice.stiereats.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * The cross-platform data layer, the Android twin of iOS FirebaseService.
 *
 * Identity: the user key is the Firebase Auth UID (from Google Sign-In —
 * stable across reinstalls, the Android analogue of iOS's iCloud id). Docs are
 * keyed by that uid using the SAME conventions iOS uses, so both platforms
 * share one dataset. Security rules gate writes on "signed in" (see
 * firestore.rules), which Google-authed users satisfy.
 *
 * Reads mirror the per-restaurant + board reads iOS cut over to in Phase 4.
 * NOTE: the board reads scan the whole `placements` collection — same cost
 * caveat as iOS; a maintained consensus doc is the eventual optimization.
 */
class FirestoreRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private val uid: String? get() = auth.currentUser?.uid

    // ── Admin exclusions (cached) ────────────────────────────────
    private var exclBannedUsers: Set<String> = emptySet()
    private var exclPlacementNames: Set<String> = emptySet()
    private var exclLoaded = false

    suspend fun loadExclusionsIfNeeded(force: Boolean = false) {
        if (exclLoaded && !force) return
        try {
            val snap = db.collection("adminExclusions").get().await()
            val banned = mutableSetOf<String>()
            val placements = mutableSetOf<String>()
            for (doc in snap.documents) {
                val kind = doc.getString("kind") ?: continue
                val target = doc.getString("target") ?: continue
                when (kind) {
                    "user" -> banned.add(target)
                    "placement" -> placements.add(target)
                }
            }
            exclBannedUsers = banned
            exclPlacementNames = placements
            exclLoaded = true
        } catch (_: Exception) { /* leave caches; unfiltered fallback */ }
    }

    private fun aggregate(
        docs: List<com.google.firebase.firestore.DocumentSnapshot>,
        keepOwner: (String) -> Boolean,
    ): Map<String, CommunityTier> {
        val sums = HashMap<String, IntArray>() // rid -> [total, count]
        for (doc in docs) {
            val rid = doc.getString("restaurantID") ?: continue
            val score = doc.getLong("score")?.toInt() ?: continue
            val owner = doc.getString("userID") ?: ""
            if (!keepOwner(owner)) continue
            if (exclBannedUsers.contains(owner)) continue
            if (exclPlacementNames.contains("placement_${owner}_$rid")) continue
            val e = sums.getOrPut(rid) { intArrayOf(0, 0) }
            e[0] += score; e[1] += 1
        }
        return sums.mapValues { (_, e) ->
            val avg = e[0].toDouble() / e[1]
            CommunityTier(Tier.fromAverage(avg), e[1], avg)
        }
    }

    // ── Community board reads ────────────────────────────────────
    suspend fun allCommunityTiers(): Map<String, CommunityTier> {
        loadExclusionsIfNeeded()
        return try {
            val snap = db.collection("placements").get().await()
            aggregate(snap.documents) { true }
        } catch (_: Exception) { emptyMap() }
    }

    suspend fun proCommunityTiers(): Map<String, CommunityTier> {
        loadExclusionsIfNeeded()
        return try {
            val proSnap = db.collection("proApprovals").get().await()
            val pros = proSnap.documents.mapNotNull { it.getString("userID") }.toSet()
            if (pros.isEmpty()) return emptyMap()
            val snap = db.collection("placements").get().await()
            aggregate(snap.documents) { pros.contains(it) }
        } catch (_: Exception) { emptyMap() }
    }

    suspend fun friendsCommunityTiers(friendIds: Set<String>): Map<String, CommunityTier> {
        if (friendIds.isEmpty()) return emptyMap()
        loadExclusionsIfNeeded()
        return try {
            val snap = db.collection("placements").get().await()
            aggregate(snap.documents) { friendIds.contains(it) }
        } catch (_: Exception) { emptyMap() }
    }

    // ── Per-restaurant reads ─────────────────────────────────────
    suspend fun communityTier(restaurantId: String): CommunityTier? {
        loadExclusionsIfNeeded()
        return try {
            val snap = db.collection("placements")
                .whereEqualTo("restaurantID", restaurantId).get().await()
            aggregate(snap.documents) { true }[restaurantId]
        } catch (_: Exception) { null }
    }

    /** Returns (tag -> count, tags the current user confirmed). */
    suspend fun dietaryTags(restaurantId: String): Pair<Map<String, Int>, Set<String>> {
        val me = uid
        return try {
            val snap = db.collection("dietaryTags")
                .whereEqualTo("restaurantID", restaurantId).get().await()
            val counts = HashMap<String, Int>()
            val mine = mutableSetOf<String>()
            for (doc in snap.documents) {
                val tag = doc.getString("tag") ?: continue
                counts[tag] = (counts[tag] ?: 0) + 1
                if (doc.getString("userID") == me) mine.add(tag)
            }
            counts to mine
        } catch (_: Exception) { emptyMap<String, Int>() to emptySet() }
    }

    /** The current user's own tier placements (restaurantId -> Tier). */
    suspend fun myPlacements(): Map<String, Tier> {
        val me = uid ?: return emptyMap()
        return try {
            val snap = db.collection("placements")
                .whereEqualTo("userID", me).get().await()
            snap.documents.mapNotNull { doc ->
                val rid = doc.getString("restaurantID") ?: return@mapNotNull null
                val tier = Tier.fromRaw(doc.getString("tier")) ?: return@mapNotNull null
                rid to tier
            }.toMap()
        } catch (_: Exception) { emptyMap() }
    }

    suspend fun visitedList(): Set<String> {
        val me = uid ?: return emptySet()
        return try {
            val doc = db.collection("visitedLists").document(me).get().await()
            (doc.get("restaurantIDs") as? List<*>)?.filterIsInstance<String>()?.toSet()
                ?: emptySet()
        } catch (_: Exception) { emptySet() }
    }

    suspend fun liveRestaurants(): List<Restaurant> = try {
        val snap = db.collection("liveRestaurants").get().await()
        snap.documents.mapNotNull { doc ->
            val name = doc.getString("name") ?: return@mapNotNull null
            val lat = doc.getDouble("latitude") ?: return@mapNotNull null
            val lon = doc.getDouble("longitude") ?: return@mapNotNull null
            Restaurant(
                id = doc.id,
                name = name,
                latitude = lat,
                longitude = lon,
                area = doc.getString("area") ?: "",
                address = doc.getString("address") ?: "",
                cuisines = (doc.get("cuisines") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                priceTier = doc.getString("priceTier") ?: "$$",
                isFastFood = doc.getBoolean("isFastFood") ?: false,
                description = doc.getString("description") ?: "",
                signatureDishes = (doc.get("signatureDishes") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                website = doc.getString("website"),
                phone = doc.getString("phone"),
            )
        }
    } catch (_: Exception) { emptyList() }

    // ── Writes ───────────────────────────────────────────────────
    suspend fun savePlacement(restaurantId: String, tier: Tier, note: String? = null) {
        val me = uid ?: return
        val data = hashMapOf<String, Any>(
            "userID" to me,
            "restaurantID" to restaurantId,
            "tier" to tier.rawValue,
            "score" to tier.score,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        note?.let {
            val t = it.trim()
            data["note"] = if (t.isEmpty()) FieldValue.delete() else t
        }
        runCatching {
            db.collection("placements").document("${me}_$restaurantId")
                .set(data, com.google.firebase.firestore.SetOptions.merge()).await()
        }
    }

    suspend fun removePlacement(restaurantId: String) {
        val me = uid ?: return
        runCatching {
            db.collection("placements").document("${me}_$restaurantId").delete().await()
        }
    }

    suspend fun setDietaryTag(restaurantId: String, tag: String, on: Boolean) {
        val me = uid ?: return
        val id = "${me}_${restaurantId}_$tag"
        runCatching {
            if (on) {
                db.collection("dietaryTags").document(id).set(
                    hashMapOf(
                        "userID" to me, "restaurantID" to restaurantId, "tag" to tag,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                    com.google.firebase.firestore.SetOptions.merge(),
                ).await()
            } else {
                db.collection("dietaryTags").document(id).delete().await()
            }
        }
    }

    suspend fun saveVisitedList(restaurantIds: Set<String>) {
        val me = uid ?: return
        runCatching {
            db.collection("visitedLists").document(me).set(
                hashMapOf(
                    "userID" to me,
                    "restaurantIDs" to restaurantIds.toList(),
                    "count" to restaurantIds.size,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                com.google.firebase.firestore.SetOptions.merge(),
            ).await()
        }
    }

    suspend fun saveClosureReport(restaurantId: String, reported: Boolean) {
        val me = uid ?: return
        val id = "${me}_$restaurantId"
        runCatching {
            if (reported) {
                db.collection("closureReports").document(id).set(
                    hashMapOf(
                        "userID" to me, "restaurantID" to restaurantId,
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                    com.google.firebase.firestore.SetOptions.merge(),
                ).await()
            } else {
                db.collection("closureReports").document(id).delete().await()
            }
        }
    }

    suspend fun saveProfile(displayName: String, status: String?) {
        val me = uid ?: return
        val data = hashMapOf<String, Any>(
            "userID" to me,
            "displayName" to displayName,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        status?.let { data["status"] = it }
        runCatching {
            db.collection("profiles").document(me)
                .set(data, com.google.firebase.firestore.SetOptions.merge()).await()
        }
    }

    // ── Account deletion (Play requirement for apps with sign-in) ────
    /**
     * Permanently deletes all of the current user's data + their Firebase Auth
     * account. Mirrors what the account-deletion web page promises. Best-effort
     * per step; the Auth-account delete may need a recent sign-in (Firebase
     * rule) — if it throws, the data is still gone and the account record ages
     * out / can be removed on next sign-in.
     */
    suspend fun deleteAccount() {
        val me = uid ?: return
        suspend fun deleteWhere(collection: String) {
            runCatching {
                val snap = db.collection(collection).whereEqualTo("userID", me).get().await()
                for (doc in snap.documents) doc.reference.delete().await()
            }
        }
        deleteWhere("placements")
        deleteWhere("dietaryTags")
        deleteWhere("closureReports")
        runCatching { db.collection("visitedLists").document(me).delete().await() }
        runCatching { db.collection("profiles").document(me).delete().await() }
        runCatching { auth.currentUser?.delete()?.await() }
        runCatching { auth.signOut() }
    }

    // ── Foodie Pro (request + admin approval) ────────────────────
    /** True if this user is flagged admin in users/{uid}.isAdmin. */
    suspend fun isAdmin(): Boolean {
        val me = uid ?: return false
        return try {
            db.collection("users").document(me).get().await().getBoolean("isAdmin") == true
        } catch (_: Exception) { false }
    }

    /** The current user's Pro standing (approved wins over requested). */
    suspend fun myProStatus(): ProStatus {
        val me = uid ?: return ProStatus.NONE
        return try {
            if (db.collection("proApprovals").document(me).get().await().exists())
                return ProStatus.APPROVED
            val profile = db.collection("profiles").document(me).get().await()
            if (profile.getString("status") == "requested") ProStatus.REQUESTED else ProStatus.NONE
        } catch (_: Exception) { ProStatus.NONE }
    }

    /** Mark the current user's profile as requesting Foodie Pro. */
    suspend fun requestFoodiePro() {
        val me = uid ?: return
        runCatching {
            db.collection("profiles").document(me).set(
                hashMapOf(
                    "userID" to me,
                    "status" to "requested",
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                com.google.firebase.firestore.SetOptions.merge(),
            ).await()
        }
    }

    data class ProUser(val userID: String, val displayName: String)
    data class ProLists(val pending: List<ProUser>, val approved: List<ProUser>)

    /** Admin: users who requested Pro (pending) + everyone approved. */
    suspend fun fetchProLists(): ProLists {
        return try {
            val reqSnap = db.collection("profiles").whereEqualTo("status", "requested").get().await()
            val apprSnap = db.collection("proApprovals").get().await()
            val approvedIds = apprSnap.documents.map { it.id }.toSet()
            val nameByUid = HashMap<String, String>()
            for (d in reqSnap.documents) {
                nameByUid[d.getString("userID") ?: d.id] = d.getString("displayName")?.ifBlank { null } ?: "(no name)"
            }
            val pending = reqSnap.documents.mapNotNull { d ->
                val u = d.getString("userID") ?: d.id
                if (u in approvedIds) null else ProUser(u, nameByUid[u] ?: "(no name)")
            }
            val approved = approvedIds.map { u -> ProUser(u, nameByUid[u] ?: u.take(10)) }
            ProLists(pending, approved)
        } catch (_: Exception) { ProLists(emptyList(), emptyList()) }
    }

    suspend fun approvePro(userID: String) {
        val me = uid ?: return
        runCatching {
            db.collection("proApprovals").document(userID).set(
                hashMapOf(
                    "userID" to userID,
                    "approvedAt" to FieldValue.serverTimestamp(),
                    "approvedBy" to me,
                ),
                com.google.firebase.firestore.SetOptions.merge(),
            ).await()
        }
    }

    suspend fun revokePro(userID: String) {
        runCatching { db.collection("proApprovals").document(userID).delete().await() }
    }

    // ── Admin dashboard stats (cheap server-side counts) ─────────
    data class AdminStats(
        val placements: Long,
        val profiles: Long,
        val approvedPros: Long,
        val pendingPros: Long,
        val closureReports: Long,
        val dietaryTags: Long,
    )

    private suspend fun count(query: com.google.firebase.firestore.Query): Long = try {
        query.count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
    } catch (_: Exception) { -1L }

    /** Server-side aggregate counts (1 read each — no full scans). */
    suspend fun adminStats(): AdminStats = AdminStats(
        placements = count(db.collection("placements")),
        profiles = count(db.collection("profiles")),
        approvedPros = count(db.collection("proApprovals")),
        pendingPros = count(db.collection("profiles").whereEqualTo("status", "requested")),
        closureReports = count(db.collection("closureReports")),
        dietaryTags = count(db.collection("dietaryTags")),
    )
}
