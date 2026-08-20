package com.compofelice.stiereats.ui

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Anchored adaptive banner, non-personalized (npa=1) to match the app's
 * no-tracking privacy stance. Shown only on Map + Browse — the tier boards
 * (Community / My Tiers) stay ad-free, mirroring iOS.
 */
private const val BANNER_UNIT_ID = "ca-app-pub-1927040492403163/5933297574"

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val widthDp = LocalConfiguration.current.screenWidthDp
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                adUnitId = BANNER_UNIT_ID
                setAdSize(
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
                )
                val npa = Bundle().apply { putString("npa", "1") }
                loadAd(
                    AdRequest.Builder()
                        .addNetworkExtrasBundle(AdMobAdapter::class.java, npa)
                        .build()
                )
            }
        },
    )
}
