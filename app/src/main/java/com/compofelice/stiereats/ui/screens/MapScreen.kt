package com.compofelice.stiereats.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.compofelice.stiereats.data.Restaurant
import com.compofelice.stiereats.ui.AppViewModel
import com.compofelice.stiereats.ui.BannerAd
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.algo.NonHierarchicalViewBasedAlgorithm
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.clustering.rememberClusterManager
import com.google.maps.android.compose.rememberCameraPositionState

/** Wraps a Restaurant as a map ClusterItem. */
private class RestaurantClusterItem(val r: Restaurant) : ClusterItem {
    private val pos = LatLng(r.latitude, r.longitude)
    override fun getPosition(): LatLng = pos
    override fun getTitle(): String = r.name
    override fun getSnippet(): String? =
        r.cuisines.firstOrNull()?.replaceFirstChar { it.uppercase() }
    override fun getZIndex(): Float = 0f
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun MapScreen(
    vm: AppViewModel,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Centered on The Woodlands, same footprint as iOS.
    val woodlands = LatLng(30.16, -95.46)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(woodlands, 10.5f)
    }
    val items = remember(vm.restaurants) { vm.restaurants.map { RestaurantClusterItem(it) } }

    Column(modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            cameraPositionState = cameraPositionState,
        ) {
            val clusterManager = rememberClusterManager<RestaurantClusterItem>()
            if (clusterManager != null) {
                val metrics = LocalContext.current.resources.displayMetrics
                // View-based algorithm: only clusters markers in the current
                // viewport instead of all ~4,700 every camera move — the key
                // perf fix. maxDistance kept at the default.
                LaunchedEffect(clusterManager) {
                    clusterManager.algorithm =
                        NonHierarchicalViewBasedAlgorithm(metrics.widthPixels, metrics.heightPixels)
                    clusterManager.setOnClusterItemClickListener { item ->
                        onOpen(item.r.id)
                        true
                    }
                }
                LaunchedEffect(clusterManager, items) {
                    clusterManager.clearItems()
                    clusterManager.addItems(items)
                    clusterManager.cluster()
                }
                Clustering(clusterManager = clusterManager)
            }
        }
        BannerAd(Modifier.fillMaxWidth())
    }
}
