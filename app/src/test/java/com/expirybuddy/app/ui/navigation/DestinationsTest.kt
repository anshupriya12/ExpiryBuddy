package com.expirybuddy.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DestinationsTest {

    @Test
    fun detailsRoute_embedsItemId() {
        assertEquals("details/42", Destinations.details(42L))
    }

    @Test
    fun editRoute_embedsItemId() {
        assertEquals("edit?itemId=7", Destinations.edit(7L))
    }

    @Test
    fun addRoute_usesNoItemSentinel() {
        assertEquals("edit?itemId=${Destinations.NO_ITEM_ID}", Destinations.add())
    }

    @Test
    fun routePatterns_doNotOverlap() {
        val patterns = listOf(
            Destinations.HOME,
            Destinations.SETTINGS,
            Destinations.DETAILS,
            Destinations.EDIT,
        )
        assertEquals(patterns.size, patterns.toSet().size)
        assertTrue(patterns.all { it.isNotBlank() })
    }
}
