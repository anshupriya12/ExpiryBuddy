package com.expirybuddy.app.domain.validation

import com.expirybuddy.app.domain.expiry.UseWithinUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ItemFormValidatorTest {
    private val date = LocalDate.of(2026, 5, 1)

    @Test
    fun validInput_hasNoErrors() {
        val result = ItemFormValidator.validate("Milk", date)
        assertTrue(result.isValid)
        assertNull(result.nameError)
        assertNull(result.dateError)
    }

    @Test
    fun emptyName_isBlankError() {
        assertEquals(NameError.BLANK, ItemFormValidator.validate("", date).nameError)
    }

    @Test
    fun whitespaceOnlyName_isBlankError() {
        assertEquals(NameError.BLANK, ItemFormValidator.validate("   \t ", date).nameError)
    }

    @Test
    fun missingDate_isMissingError() {
        assertEquals(DateError.MISSING, ItemFormValidator.validate("Milk", null).dateError)
    }

    @Test
    fun pastDate_isAllowed_soExpiredItemsCanBeTracked() {
        assertTrue(ItemFormValidator.validate("Old jar", LocalDate.of(2000, 1, 1)).isValid)
    }

    @Test
    fun bothInvalid_reportsBothErrors() {
        val result = ItemFormValidator.validate("", null)
        assertFalse(result.isValid)
        assertEquals(NameError.BLANK, result.nameError)
        assertEquals(DateError.MISSING, result.dateError)
    }

    @Test
    fun useWithin_blankIsFine_validNumbersPass_badOnesFail() {
        assertNull(ItemFormValidator.validate("Milk", date, useWithinText = "").useWithinError)
        assertNull(ItemFormValidator.validate("Milk", date, useWithinText = " 2 ").useWithinError)
        listOf("0", "-1", "abc", "2.5", "99999").forEach {
            assertEquals(it, UseWithinError.INVALID, ItemFormValidator.validate("Milk", date, useWithinText = it).useWithinError)
        }
    }

    @Test
    fun useWithin_limitsDependOnTheUnit() {
        assertNull(ItemFormValidator.validate("Milk", date, "120", UseWithinUnit.MONTHS).useWithinError)
        assertEquals(UseWithinError.INVALID, ItemFormValidator.validate("Milk", date, "121", UseWithinUnit.MONTHS).useWithinError)
        assertNull(ItemFormValidator.validate("Milk", date, "121", UseWithinUnit.DAYS).useWithinError)
    }

    @Test
    fun customReminder_mustBeAWholeNumberOfDaysUpTo365() {
        assertNull(ItemFormValidator.validate("Milk", date).customReminderError)
        assertNull(ItemFormValidator.validate("Milk", date, customReminderText = "5").customReminderError)
        assertNull(ItemFormValidator.validate("Milk", date, customReminderText = "365").customReminderError)
        listOf("", "0", "366", "x").forEach {
            assertEquals(it, CustomReminderError.INVALID, ItemFormValidator.validate("Milk", date, customReminderText = it).customReminderError)
        }
        assertFalse(ItemFormValidator.validate("Milk", date, customReminderText = "0").isValid)
    }
}
