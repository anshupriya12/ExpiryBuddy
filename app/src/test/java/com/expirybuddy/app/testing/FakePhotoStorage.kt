package com.expirybuddy.app.testing

import com.expirybuddy.app.data.photo.PhotoStorage

/** In-memory [PhotoStorage]: "imports" produce numbered URIs and every delete is recorded. */
class FakePhotoStorage : PhotoStorage {
    private var counter = 0

    /** URIs that currently "exist" in storage. */
    val stored = linkedSetOf<String>()
    val deleted = mutableListOf<String>()
    var failImport = false
    var camera = true
    var lastSweepKept: Set<String>? = null

    override suspend fun importPhoto(sourceUri: String): String? {
        if (failImport) return null
        return "file:///photos/${++counter}.jpg".also { stored += it }
    }

    override fun delete(photoUri: String?) {
        if (photoUri == null) return
        stored -= photoUri
        deleted += photoUri
    }

    override suspend fun deleteAllExcept(keep: Set<String>) {
        lastSweepKept = keep
        stored.retainAll(keep)
    }

    override fun createCameraTarget(): String? = if (camera) "content://camera/target.jpg" else null

    override fun hasCamera(): Boolean = camera
}
