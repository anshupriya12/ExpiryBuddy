package com.expirybuddy.app.reminders

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.expirybuddy.app.ExpiryBuddyApp
import com.expirybuddy.app.domain.model.ExpiryItem
import com.expirybuddy.app.domain.model.ItemCategory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

/** Runs the real worker against the real database and checks which notifications actually appear. */
@RunWith(AndroidJUnit4::class)
class ReminderWorkerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val container = (context.applicationContext as ExpiryBuddyApp).container
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val today = LocalDate.now()
    private val createdIds = mutableListOf<Long>()

    @Before
    fun setUp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        NotificationHelper.createChannel(context)
        notificationManager.cancelAll()
    }

    @After
    fun tearDown() = runBlocking {
        // Deleting through the repository also cancels any reminder it scheduled.
        createdIds.forEach { container.itemRepository.delete(it) }
        notificationManager.cancelAll()
    }

    private fun saveItem(expiresInDays: Long, daysBefore: Int?): Long = runBlocking {
        container.itemRepository.save(
            ExpiryItem(
                name = "Worker test milk",
                category = ItemCategory.FOOD,
                expiryDate = today.plusDays(expiresInDays),
                reminderDaysBefore = daysBefore,
                createdAt = Instant.now(),
            ),
        ).also { createdIds += it }
    }

    private fun runWorker(itemId: Long): ListenableWorker.Result = runBlocking {
        TestListenableWorkerBuilder<ReminderWorker>(context)
            .setInputData(workDataOf(ReminderWorker.KEY_ITEM_ID to itemId))
            .build()
            .doWork()
    }

    private fun shown(itemId: Long) =
        notificationManager.activeNotifications.firstOrNull { it.id == NotificationHelper.notificationId(itemId) }

    /** The system registers a posted notification asynchronously, so wait briefly before asserting one exists. */
    private fun awaitShown(itemId: Long): android.service.notification.StatusBarNotification? {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            shown(itemId)?.let { return it }
            Thread.sleep(50)
        }
        return null
    }

    @Test
    fun relevantReminder_postsANotificationWithTheItemNameAndCountdown() {
        val id = saveItem(expiresInDays = 2, daysBefore = 2) // reminder day is today

        val result = runWorker(id)

        assertEquals(ListenableWorker.Result.success(), result)
        val posted = awaitShown(id)
        assertTrue("expected a notification", posted != null)
        val extras = posted!!.notification.extras
        assertEquals("Worker test milk", extras.getCharSequence(NotificationCompat.EXTRA_TITLE).toString())
        assertEquals("Use within 2 days", extras.getCharSequence(NotificationCompat.EXTRA_TEXT).toString())
    }

    @Test
    fun dueToday_saysUseByToday() {
        val id = saveItem(expiresInDays = 0, daysBefore = 0)

        runWorker(id)

        val text = awaitShown(id)!!.notification.extras.getCharSequence(NotificationCompat.EXTRA_TEXT).toString()
        assertEquals("Use by today", text)
    }

    @Test
    fun deletedItem_postsNothing() {
        val id = saveItem(expiresInDays = 2, daysBefore = 2)
        runBlocking { container.itemRepository.delete(id) }

        val result = runWorker(id)

        assertEquals(ListenableWorker.Result.success(), result)
        assertFalse(shown(id) != null)
    }

    @Test
    fun reminderDayNotReachedYet_postsNothing() {
        val id = saveItem(expiresInDays = 10, daysBefore = 3) // reminder is a week away

        runWorker(id)

        assertFalse(shown(id) != null)
    }

    @Test
    fun alreadyExpiredItem_postsNothing() {
        val id = saveItem(expiresInDays = -1, daysBefore = 0)

        runWorker(id)

        assertFalse(shown(id) != null)
    }

    @Test
    fun itemMarkedAsUsed_postsNothing_evenIfTheJobStillFires() {
        val id = saveItem(expiresInDays = 2, daysBefore = 2) // reminder day is today
        runBlocking {
            val item = container.itemRepository.getItem(id)!!
            container.itemRepository.save(item.copy(usedAt = Instant.now()))
        }

        val result = runWorker(id)

        assertEquals(ListenableWorker.Result.success(), result)
        assertFalse(shown(id) != null)
        // And the item still exists: marking as used is not deleting.
        assertTrue(runBlocking { container.itemRepository.getItem(id) } != null)
    }

    @Test
    fun itemEditedToHaveNoReminder_postsNothing() {
        val id = saveItem(expiresInDays = 2, daysBefore = 2)
        runBlocking {
            container.itemRepository.save(container.itemRepository.getItem(id)!!.copy(reminderDaysBefore = null))
        }

        runWorker(id)

        assertFalse(shown(id) != null)
    }

    @Test
    fun missingItemIdInInput_fails() {
        val result = runBlocking {
            TestListenableWorkerBuilder<ReminderWorker>(context).build().doWork()
        }
        assertEquals(ListenableWorker.Result.failure(), result)
    }
}
