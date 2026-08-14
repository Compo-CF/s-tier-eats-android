package com.compofelice.stiereats.data

import android.content.Context
import org.json.JSONObject

/**
 * Loads the restaurant catalog: the bundled seed (assets/restaurants.json,
 * the same file iOS ships) merged with community-approved additions from the
 * Firestore `liveRestaurants` collection. Parsed with org.json (built into
 * Android — no extra serialization dependency).
 */
class CatalogRepository(private val context: Context) {

    fun loadBundled(): List<Restaurant> {
        val text = context.assets.open("restaurants.json")
            .bufferedReader().use { it.readText() }
        val arr = JSONObject(text).getJSONArray("restaurants")
        val out = ArrayList<Restaurant>(arr.length())
        for (i in 0 until arr.length()) out.add(parse(arr.getJSONObject(i)))
        return out
    }

    private fun parse(o: JSONObject): Restaurant {
        fun strOrNull(key: String): String? =
            if (o.isNull(key)) null else o.optString(key).ifEmpty { null }

        val cuisines = o.optJSONArray("cuisines")?.let { a ->
            (0 until a.length()).map { a.getString(it) }
        } ?: emptyList()
        val dishes = o.optJSONArray("signatureDishes")?.let { a ->
            (0 until a.length()).map { a.getString(it) }
        } ?: emptyList()

        return Restaurant(
            id = o.getString("id"),
            name = o.getString("name"),
            latitude = o.getDouble("latitude"),
            longitude = o.getDouble("longitude"),
            area = o.optString("area"),
            address = o.optString("address"),
            cuisines = cuisines,
            priceTier = o.optString("priceTier", "$$"),
            isFastFood = o.optBoolean("isFastFood", false),
            description = o.optString("description"),
            signatureDishes = dishes,
            website = strOrNull("website"),
            phone = strOrNull("phone"),
        )
    }
}
