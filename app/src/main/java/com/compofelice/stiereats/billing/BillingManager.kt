package com.compofelice.stiereats.billing

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * Play Billing wrapper for the "S-Tier Plus" subscription — removes ads for
 * subscribers (and is the hook for future Plus-only perks). Entitlement state
 * ([isPlus]) and the formatted [priceText] are Compose state so the UI reacts.
 *
 * The product `stier_plus_monthly` must be created in Play Console (an
 * auto-renewing subscription with a base plan) — until it exists,
 * queryProductDetails returns empty and the paywall shows "unavailable",
 * exactly like the iOS new-IAP flow.
 */
class BillingManager(context: Context) {

    companion object {
        const val PRODUCT_ID = "stier_plus_monthly"
    }

    /** True while the user holds an active S-Tier Plus subscription. */
    var isPlus by mutableStateOf(false)
        private set

    /** Localized price (e.g. "$2.99") once the product is fetched, else null. */
    var priceText by mutableStateOf<String?>(null)
        private set

    private var productDetails: ProductDetails? = null

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { handlePurchase(it) }
        }
    }

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    /** Connect and refresh product + entitlement. Safe to call again on resume. */
    fun start() {
        if (client.isReady) {
            queryProduct(); queryEntitlement(); return
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProduct()
                    queryEntitlement()
                }
            }
            override fun onBillingServiceDisconnected() { /* next start() reconnects */ }
        })
    }

    private fun queryProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()
        client.queryProductDetailsAsync(params) { result, list ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = list.firstOrNull()
                priceText = productDetails
                    ?.subscriptionOfferDetails?.firstOrNull()
                    ?.pricingPhases?.pricingPhaseList?.firstOrNull()
                    ?.formattedPrice
            }
        }
    }

    private fun queryEntitlement() {
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            val active = purchases.any {
                it.purchaseState == Purchase.PurchaseState.PURCHASED && PRODUCT_ID in it.products
            }
            isPlus = active
            purchases.forEach {
                if (it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged) acknowledge(it)
            }
        }
    }

    /** Launch the Play purchase sheet. No-op if the product isn't loaded yet. */
    fun launchPurchase(activity: Activity) {
        val pd = productDetails ?: return
        val offerToken = pd.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(pd)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()
        client.launchBillingFlow(activity, flowParams)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && PRODUCT_ID in purchase.products) {
            isPlus = true
            if (!purchase.isAcknowledged) acknowledge(purchase)
        }
    }

    private fun acknowledge(purchase: Purchase) {
        client.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        ) { /* best-effort */ }
    }
}
