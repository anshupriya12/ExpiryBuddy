package com.expirybuddy.app.data.photo

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.math.max

/** [PhotoStorage] backed by `files/photos` (kept) and `cache/camera` (temporary camera output). */
class FilePhotoStorage(context: Context) : PhotoStorage {
    private val appContext = context.applicationContext
    private val photosDir = File(appContext.filesDir, PHOTOS_DIR)
    private val cameraDir = File(appContext.cacheDir, CAMERA_DIR)

    /** Outlives any ViewModel, so a delete requested as a screen closes still completes. */
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun importPhoto(sourceUri: String): String? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(sourceUri)
            val bitmap = decodeDownsized(uri) ?: return@withContext null
            val upright = bitmap.rotatedByExif(uri)
            photosDir.mkdirs()
            val target = File(photosDir, "${UUID.randomUUID()}.jpg")
            target.outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            Uri.fromFile(target).toString()
        } catch (e: IOException) {
            Log.w(TAG, "Could not read the picture", e)
            null
        } catch (e: SecurityException) {
            Log.w(TAG, "No permission to read the picture", e)
            null
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "Picture too large to prepare", e)
            null
        } finally {
            clearCameraCache()
        }
    }

    override fun delete(photoUri: String?) {
        val file = fileFor(photoUri) ?: return
        ioScope.launch { file.delete() }
    }

    override suspend fun deleteAllExcept(keep: Set<String>) {
        withContext(Dispatchers.IO) {
            val keptFiles = keep.mapNotNull(::fileFor).map { it.canonicalPath }.toSet()
            photosDir.listFiles()?.forEach { file ->
                if (file.canonicalPath !in keptFiles) file.delete()
            }
            clearCameraCache()
        }
    }

    override fun createCameraTarget(): String? = try {
        cameraDir.mkdirs()
        val file = File(cameraDir, "capture-${UUID.randomUUID()}.jpg")
        FileProvider.getUriForFile(appContext, authority(), file).toString()
    } catch (e: IllegalArgumentException) {
        null
    }

    override fun hasCamera(): Boolean =
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(appContext.packageManager) != null

    private fun authority() = "${appContext.packageName}.fileprovider"

    /** Only files inside [photosDir] are ever touched, whatever string is passed in. */
    private fun fileFor(photoUri: String?): File? {
        val path = photoUri?.let { Uri.parse(it).path } ?: return null
        val file = File(path)
        return file.takeIf { it.canonicalFile.parentFile == photosDir.canonicalFile }
    }

    private fun clearCameraCache() {
        cameraDir.listFiles()?.forEach { it.delete() }
    }

    private fun decodeDownsized(uri: Uri): Bitmap? {
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // decodeStream returns null when it only measures (inJustDecodeBounds), so the result is ignored
        // here and "could not open the stream" is detected separately.
        val opened = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
            true
        } ?: return null
        if (!opened || bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE_PX) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return null

        val longest = max(decoded.width, decoded.height)
        if (longest <= MAX_SIDE_PX) return decoded
        val scale = MAX_SIDE_PX.toFloat() / longest
        return Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    /** Camera pictures are often stored sideways with an EXIF flag; bake the rotation in. */
    private fun Bitmap.rotatedByExif(uri: Uri): Bitmap {
        val orientation = appContext.contentResolver.openInputStream(uri)?.use {
            android.media.ExifInterface(it).getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: return this
        val matrix = Matrix()
        when (orientation) {
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return this
        }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private companion object {
        const val TAG = "PhotoStorage"
        const val PHOTOS_DIR = "photos"
        const val CAMERA_DIR = "camera"
        const val MAX_SIDE_PX = 1280
        const val JPEG_QUALITY = 85
    }
}
