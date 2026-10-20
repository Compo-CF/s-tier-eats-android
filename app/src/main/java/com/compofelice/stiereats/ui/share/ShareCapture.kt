package com.compofelice.stiereats.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/** Play Store listing — appended to every shared card so the image carries a
 *  one-tap install link (the app's main growth channel is link/image shares). */
const val PLAY_STORE_URL =
    "https://play.google.com/store/apps/details?id=com.compofelice.stiereats"

/**
 * Draw this composable's content into [layer] (as well as to the screen) so it
 * can be captured as a bitmap. Mirrors the iOS `ImageRenderer` share-card
 * pattern: the card is shown in a preview sheet and captured on demand.
 *
 * Usage: `Box(Modifier.captureToLayer(layer)) { TierShareCard(...) }`, then
 * `shareCapturedCard(context, layer)` from a coroutine.
 */
fun Modifier.captureToLayer(layer: GraphicsLayer): Modifier = drawWithContent {
    // Record this node's drawing into the layer, then paint the layer to screen.
    layer.record { this@drawWithContent.drawContent() }
    drawLayer(layer)
}

/**
 * Export a recorded [layer] to a PNG in `cacheDir/shared_images` and launch the
 * system share chooser with that image + a Play Store link. Suspends while the
 * layer is rasterized and the file written. The layer must have been drawn at
 * least once (it is, as soon as the preview card appears) before calling this.
 */
suspend fun shareCapturedCard(
    context: Context,
    layer: GraphicsLayer,
    fileName: String = "stier-share.png",
    shareText: String = "My S-Tier Eats tier list — rank your local restaurants. $PLAY_STORE_URL",
) {
    val bitmap: Bitmap = layer.toImageBitmap().asAndroidBitmap()
    val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
    val file = File(dir, fileName)
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, shareText)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        Intent.createChooser(send, "Share").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    )
}
