package com.expirybuddy.app.data.photo

/**
 * Keeps item photos on the device. Photos are copied into the app's private storage (downsized, so a
 * 12 MP camera picture does not become a 5 MB file) and referenced by a URI string, so they keep
 * working after the original is moved or deleted and nothing needs a storage permission.
 *
 * URIs are plain strings so ViewModels and their tests stay free of Android types.
 */
interface PhotoStorage {

    /**
     * Copies and downsizes the image at [sourceUri] (a gallery or camera URI) into private storage.
     * Returns the stored photo's URI, or null if the image could not be read.
     */
    suspend fun importPhoto(sourceUri: String): String?

    /**
     * Deletes a stored photo in the background. Ignores null, unknown and already-deleted URIs.
     * Not suspending on purpose: ViewModels call it from `onCleared`, where no coroutine is available.
     */
    fun delete(photoUri: String?)

    /** Deletes every stored photo whose URI is not in [keep]. Used to clean up after crashes. */
    suspend fun deleteAllExcept(keep: Set<String>)

    /** A fresh URI the camera app can write a picture to, or null if one cannot be created. */
    fun createCameraTarget(): String?

    /** Whether a camera app is available to take a picture. */
    fun hasCamera(): Boolean
}
