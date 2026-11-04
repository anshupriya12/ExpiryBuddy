package com.expirybuddy.app.domain.validation

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
}
