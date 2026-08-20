package com.compofelice.stiereats

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

/**
 * Application entry point. Firebase auto-initializes via the google-services
 * plugin + google-services.json. Here we also boot the AdMob SDK with the
 * account-wide General content-rating cap (matches the iOS apps — keeps
 * adult/suggestive creative off a food app).
 */
class STierApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build()
        )
        MobileAds.initialize(this) {}
    }
}
