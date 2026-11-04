package com.expirybuddy.app.ui.navigation

/**
 * Central place for navigation routes. Plain string routes keep the dependency set small;
 * the helpers below are the only place routes are built, and they are unit-tested.
 */
object Destinations {
    const val ITEM_ID_ARG = "itemId"

    /** Sentinel used by the Add/Edit route when no item is being edited. */
    const val NO_ITEM_ID = -1L

    const val HOME = "home"
    const val SETTINGS = "settings"
    const val DETAILS = "details/{$ITEM_ID_ARG}"
    const val EDIT = "edit?$ITEM_ID_ARG={$ITEM_ID_ARG}"

    fun details(itemId: Long): String = "details/$itemId"

    fun add(): String = "edit?$ITEM_ID_ARG=$NO_ITEM_ID"

    fun edit(itemId: Long): String = "edit?$ITEM_ID_ARG=$itemId"
}
