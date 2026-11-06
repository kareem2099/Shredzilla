package com.FreeRave.shredzilla.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MARGIN_FRACTION = 0.06f

/**
 * Shares [image] through the system share sheet. It's framed on [backgroundColor] first, since
 * apps like WhatsApp turn transparent rounded corners black.
 */
suspend fun shareImage(context: Context, image: Bitmap, backgroundColor: Int, chooserTitle: String) {
    val uri = withContext(Dispatchers.IO) {
        // Layer snapshots are hardware bitmaps, which a software canvas can't draw
        val card = image.copy(Bitmap.Config.ARGB_8888, false)
        val margin = (card.width * MARGIN_FRACTION).toInt()
        val framed = Bitmap.createBitmap(card.width + 2 * margin, card.height + 2 * margin, Bitmap.Config.ARGB_8888)
        Canvas(framed).apply {
            drawColor(backgroundColor)
            drawBitmap(card, margin.toFloat(), margin.toFloat(), null)
        }
        // cache/images/ is the folder file_paths.xml exposes through the FileProvider
        val file = File(File(context.cacheDir, "images").apply { mkdirs() }, "shredzilla_share.png")
        file.outputStream().use { framed.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
