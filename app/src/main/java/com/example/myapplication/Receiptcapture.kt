package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File


private fun receiptsDir(context: Context): File =
    File(context.filesDir, "receipts").apply { mkdirs() }

fun receiptFileFor(context: Context, id: String): File =
    File(receiptsDir(context), "$id.jpg")

fun receiptUriFor(context: Context, id: String): Uri =
    FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        receiptFileFor(context, id)
    )

fun decodeSampledReceiptBitmap(file: File, reqWidth: Int = 800, reqHeight: Int = 800): Bitmap? {
    if (!file.exists()) return null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var inSampleSize = 1
    val halfHeight = bounds.outHeight / 2
    val halfWidth = bounds.outWidth / 2
    while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
        inSampleSize *= 2
    }

    val decodeOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
    return BitmapFactory.decodeFile(file.path, decodeOptions)
}