package com.expirybuddy.app.domain.expiry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class UseByCalculatorTest {
    private val opened = LocalDate.of(2026, 11, 12)

    @Test
    fun openedOn12Nov_useWithin2Days_isThe14th() {
        assertEquals(LocalDate.of(2026, 11, 14), UseByCalculator.calculate(opened, 2, UseWithinUnit.DAYS))
    }

    @Test
    fun days_cross_monthAndYearBoundaries() {
        assertEquals(LocalDate.of(2027, 1, 2), UseByCalculator.calculate(LocalDate.of(2026, 12, 28), 5, UseWithinUnit.DAYS))
        assertEquals(LocalDate.of(2028, 2, 29), UseByCalculator.calculate(LocalDate.of(2028, 2, 28), 1, UseWithinUnit.DAYS))
    }

    @Test
    fun months_followTheCalendar_andClampToTheEndOfShortMonths() {
        assertEquals(LocalDate.of(2027, 2, 12), UseByCalculator.calculate(opened, 3, UseWithinUnit.MONTHS))
        assertEquals(LocalDate.of(2026, 2, 28), UseByCalculator.calculate(LocalDate.of(2026, 1, 31), 1, UseWithinUnit.MONTHS))
        assertEquals(LocalDate.of(2028, 2, 29), UseByCalculator.calculate(LocalDate.of(2028, 1, 31), 1, UseWithinUnit.MONTHS))
    }

    @Test
    fun missingOpeningDateOrAmount_givesNoResult() {
        assertNull(UseByCalculator.calculate(null, 2, UseWithinUnit.DAYS))
        assertNull(UseByCalculator.calculate(opened, null, UseWithinUnit.DAYS))
    }

    @Test
    fun invalidAmounts_giveNoResult() {
        assertNull(UseByCalculator.calculate(opened, 0, UseWithinUnit.DAYS))
        assertNull(UseByCalculator.calculate(opened, -3, UseWithinUnit.DAYS))
        assertNull(UseByCalculator.calculate(opened, 3651, UseWithinUnit.DAYS))
        assertNull(UseByCalculator.calculate(opened, 121, UseWithinUnit.MONTHS))
    }

    @Test
    fun amountLimits_areInclusive() {
        assertTrue(UseByCalculator.isValidAmount(1, UseWithinUnit.DAYS))
        assertTrue(UseByCalculator.isValidAmount(3650, UseWithinUnit.DAYS))
        assertTrue(UseByCalculator.isValidAmount(120, UseWithinUnit.MONTHS))
        assertFalse(UseByCalculator.isValidAmount(null, UseWithinUnit.DAYS))
    }

    @Test
    fun unitStorageKeys_roundTrip_andUnknownFallsBackToDays() {
        UseWithinUnit.entries.forEach { assertEquals(it, UseWithinUnit.fromStorageKey(it.storageKey)) }
        assertEquals(UseWithinUnit.DAYS, UseWithinUnit.fromStorageKey(null))
        assertEquals(UseWithinUnit.DAYS, UseWithinUnit.fromStorageKey("weeks"))
    }
}
