package com.expirybuddy.app.ui.navigation

/**
 * Central place for navigation routes. Plain string routes keep the dependency set small;
 * the helpers below are the only place routes are built, and they are unit-tested.
 */
object Destinations {
    const val ITEM_ID_ARG = "itemId"

    /** Sentinel used by the Add/Edit route when no item is being edited. */
    const val NO_ITEM_ID = -1L

    /**
     * Key under which a screen leaves a one-time confirmation message (a string resource id) for the
     * screen below it on the back stack, to be shown as a snackbar.
     */
    const val MESSAGE_KEY = "message"

    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val ALL_ITEMS = "all_items"
    const val SETTINGS = "settings"
    const val DETAILS = "details/{$ITEM_ID_ARG}"
    const val EDIT = "edit?$ITEM_ID_ARG={$ITEM_ID_ARG}"

    fun details(itemId: Long): String = "details/$itemId"

    fun add(): String = "edit?$ITEM_ID_ARG=$NO_ITEM_ID"

    fun edit(itemId: Long): String = "edit?$ITEM_ID_ARG=$itemId"
}
