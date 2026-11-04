package com.expirybuddy.app.reminders

import com.expirybuddy.app.domain.model.ExpiryItem

interface ReminderScheduler {
    /**
     * Makes the pending reminder for [item] match its current data. Any previous reminder for the
     * item is replaced; if the item now has no reminder, or its reminder moment has already
     * passed, the previous reminder is cancelled instead of being left behind.
     */
    fun schedule(item: ExpiryItem)

    /** Cancels the pending reminder for the item with [itemId], if any. */
    fun cancel(itemId: Long)

    /**
     * Re-enqueues reminders that are still in the future, e.g. at app start, to correct drift after
     * a time-zone change. Deliberately never cancels: at start-up a reminder may be firing right now.
     */
    fun restore(items: List<ExpiryItem>)
}

object ReminderWork {
    /** Stable unique work name for an item, so each item has at most one pending reminder. */
    fun uniqueName(itemId: Long): String = "reminder-item-$itemId"
}
