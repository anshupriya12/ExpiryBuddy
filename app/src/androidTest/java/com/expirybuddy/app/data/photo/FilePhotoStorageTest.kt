package com.expirybuddy.app.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Runs the real photo storage on a device: importing, downsizing, and deleting only what it owns. */
@RunWith(AndroidJUnit4::class)
class FilePhotoStorageTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val storage = FilePhotoStorage(context)
    private val photosDir = File(context.filesDir, "photos")
    private val scratchDir = File(context.cacheDir, "photo-test")

    @Before
    fun setUp() {
        scratchDir.mkdirs()
        photosDir.listFiles()?.forEach { it.delete() }
    }

    @After
    fun tearDown() {
        scratchDir.deleteRecursively()
        photosDir.listFiles()?.forEach { it.delete() }
    }

    private fun jpeg(name: String, width: Int, height: Int): File {
        val file = File(scratchDir, name)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(245, 106, 42)) }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        return file
    }

    private fun fileOf(uri: String) = File(Uri.parse(uri).path!!)

    /** Deletion runs in the background, so wait briefly for the file to disappear. */
    private fun awaitGone(file: File): Boolean {
        val deadline = System.currentTimeMillis() + 3_000
        while (System.currentTimeMillis() < deadline) {
            if (!file.exists()) return true
            Thread.sleep(50)
        }
        return !file.exists()
    }

    @Test
    fun import_copiesIntoPrivateStorage_andDownsizesLargePictures() = runBlocking {
        val source = jpeg("big.jpg", 3000, 2000)

        val stored = storage.importPhoto(Uri.fromFile(source).toString())

        assertNotNull(stored)
        val file = fileOf(stored!!)
        assertEquals(photosDir.canonicalFile, file.canonicalFile.parentFile)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        assertTrue("longest side is at most 1280px but was ${bounds.outWidth}x${bounds.outHeight}", maxOf(bounds.outWidth, bounds.outHeight) <= 1280)
        assertTrue("aspect ratio is kept", bounds.outWidth > bounds.outHeight)
        // The original is untouched: it belongs to the gallery or the camera, not to us.
        assertTrue(source.exists())
    }

    @Test
    fun import_keepsSmallPicturesAtTheirSize() = runBlocking {
        val stored = storage.importPhoto(Uri.fromFile(jpeg("small.jpg", 400, 300)).toString())!!

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(fileOf(stored).path, bounds)
        assertEquals(400, bounds.outWidth)
        assertEquals(300, bounds.outHeight)
    }

    @Test
    fun import_ofSomethingUnreadable_returnsNull_andLeavesNoFile() = runBlocking {
        val notAnImage = File(scratchDir, "notes.txt").apply { writeText("not an image") }

        assertNull(storage.importPhoto(Uri.fromFile(notAnImage).toString()))
        assertNull(storage.importPhoto("content://does.not.exist/photo"))
        assertEquals(0, photosDir.listFiles()?.size ?: 0)
    }

    @Test
    fun delete_removesAStoredPhoto() = runBlocking {
        val stored = storage.importPhoto(Uri.fromFile(jpeg("a.jpg", 200, 200)).toString())!!
        val file = fileOf(stored)
        assertTrue(file.exists())

        storage.delete(stored)

        assertTrue(awaitGone(file))
    }

    @Test
    fun delete_ignoresNullAndFilesOutsideThePhotosFolder() {
        val outsider = jpeg("keep-me.jpg", 50, 50)

        storage.delete(null)
        storage.delete(Uri.fromFile(outsider).toString())
        storage.delete("not even a uri")
        Thread.sleep(300)

        assertTrue("a file outside files/photos must never be deleted", outsider.exists())
    }

    @Test
    fun deleteAllExcept_keepsOnlyTheListedPhotos() = runBlocking {
        val keep = storage.importPhoto(Uri.fromFile(jpeg("1.jpg", 100, 100)).toString())!!
        val drop = storage.importPhoto(Uri.fromFile(jpeg("2.jpg", 100, 100)).toString())!!

        storage.deleteAllExcept(setOf(keep))

        assertTrue(fileOf(keep).exists())
        assertFalse(fileOf(drop).exists())
    }

    @Test
    fun cameraTarget_isAContentUriForTheAppsFileProvider() {
        val target = storage.createCameraTarget()

        assertNotNull(target)
        assertTrue(target!!.startsWith("content://${context.packageName}.fileprovider/"))
    }
}
