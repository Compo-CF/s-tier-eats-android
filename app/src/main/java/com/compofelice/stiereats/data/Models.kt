package com.compofelice.stiereats.data

/**
 * Core domain models, mirroring the iOS app's enums/structs. These MUST stay
 * wire-compatible with the Firestore schema written by iOS
 * (docs/firestore-schema.md in the woodlands-eats repo) — same rawValues,
 * same field names, same doc-id conventions — so both platforms read/write
 * one shared dataset.
 */

/** S/A/B/C/F tier. rawValue + score match iOS Tier exactly. */
enum class Tier(val rawValue: String, val score: Int) {
    S("S", 5),
    A("A", 4),
    B("B", 3),
    C("C", 2),
    F("F", 1);

    companion object {
        fun fromRaw(raw: String?): Tier? = entries.firstOrNull { it.rawValue == raw }

        /**
         * Consensus tier from an average score. Round-to-nearest — VERIFY this
         * matches iOS `Tier.from(averageScore:)` in Enums.swift before relying
         * on cross-platform board parity.
         */
        fun fromAverage(avg: Double): Tier = when {
            avg >= 4.5 -> S
            avg >= 3.5 -> A
            avg >= 2.5 -> B
            avg >= 1.5 -> C
            else -> F
        }
    }
}

/** Community consensus for one restaurant. */
data class CommunityTier(
    val tier: Tier,
    val count: Int,
    val average: Double,
)

/** Crowdsourced dietary/needs tags (v2.4 on iOS). rawValue matches iOS DietaryTag. */
enum class DietaryTag(val rawValue: String, val displayName: String) {
    GLUTEN_FREE("glutenFree", "Gluten-free"),
    VEGETARIAN("vegetarian", "Vegetarian"),
    VEGAN("vegan", "Vegan"),
    ALLERGY_AWARE("allergyAware", "Allergy-aware"),
    HALAL("halal", "Halal"),
    KID_FRIENDLY("kidFriendly", "Kid-friendly"),
    KOSHER("kosher", "Kosher");

    companion object {
        fun fromRaw(raw: String?): DietaryTag? = entries.firstOrNull { it.rawValue == raw }
    }
}

/**
 * A restaurant. The catalog itself ships bundled (like iOS's Restaurants.json)
 * plus community-approved additions from the `liveRestaurants` Firestore
 * collection. UUID string id matches iOS.
 */
data class Restaurant(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val area: String,
    val address: String,
    val cuisines: List<String>,
    val priceTier: String,
    val isFastFood: Boolean,
    val description: String,
    val signatureDishes: List<String> = emptyList(),
    val website: String? = null,
    val phone: String? = null,
)
