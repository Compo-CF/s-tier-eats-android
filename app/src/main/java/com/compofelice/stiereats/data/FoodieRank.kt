package com.compofelice.stiereats.data

/**
 * Foodie rank progression, mirroring iOS `FoodieRank`. Thresholds, names, and
 * blurbs match the iOS enum exactly so the two platforms award the same rank
 * for the same placement count.
 */
enum class FoodieRank(
    val minPlacements: Int,
    val displayName: String,
    val blurb: String,
) {
    NEWCOMER(1, "Newcomer", "Just trying things out."),
    FOODIE(5, "Foodie", "Building a real list."),
    CRITIC(15, "Critic", "Opinions you can trust."),
    CONNOISSEUR(30, "Connoisseur", "Deep local knowledge."),
    TASTEMAKER(60, "Tastemaker", "Shapes the local scene.");

    companion object {
        /** The rank for a placement count, or null when the user has ranked nothing yet. */
        fun from(placementCount: Int): FoodieRank? {
            if (placementCount < 1) return null
            return entries.reversed().firstOrNull { placementCount >= it.minPlacements }
        }
    }
}
