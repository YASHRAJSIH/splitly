package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * FILE PURPOSE: Where a captured receipt photo lives on disk, per Add Expense
 * session.
 *
 * Stored under the app's own private files dir (context.filesDir/receipts/),
 * never Firebase — "sort it locally" means exactly that: no upload, no
 * Firebase Storage dependency. Each photo is named after a random id
 * generated once when AddExpenseScreen opens, purely so this screen can save
 * and immediately re-load its own preview.
 *
 * SCOPE: this is intentionally self-contained to the Add Expense screen only.
 * The random id is NOT linked back to the expense once it's saved to
 * Firebase, so the photo will not show up again later if you open that
 * expense from the person's transaction history — that would require this
 * screen to learn the expense's generated Firebase key after saving and
 * rename the file to match, which also means changing uploadPersonExpenses'
 * callback in Firebase.kt. Left out for now; ask if you want that wired up.
 *
 * NOTE: this is per-device, on purpose. Reinstalling the app or switching
 * phones loses these photos — only the expense data (which lives in
 * Firebase) travels with the account.
 *
 * Needs a FileProvider declared in AndroidManifest.xml (see the setup
 * instructions given alongside this file) — the camera app can't write
 * directly into another app's private storage without one.
 */

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

/**
 * Decodes a receipt photo downsampled to roughly reqWidth x reqHeight
 * instead of full camera resolution — a full 12MP+ photo decoded straight
 * into a Bitmap for a small preview is a needless amount of memory (and a
 * real OOM risk on lower-end phones). Returns null if there's no photo yet,
 * or the file is unreadable.
 */
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