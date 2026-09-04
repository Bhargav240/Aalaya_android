package com.aalaya.pg.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Exact BigDecimal-based rupee parsing — no floating-point drift. */
class MoneyParseTest {

    @Test
    fun `parses whole rupees`() {
        assertEquals(1_500_00L, Money.parseRupeesToPaise("1500"))
    }

    @Test
    fun `parses decimals exactly`() {
        assertEquals(1_234_56L, Money.parseRupeesToPaise("1234.56"))
        // The classic Double-precision trap: 0.29 * 100 != 29 in floating point.
        assertEquals(29L, Money.parseRupeesToPaise("0.29"))
    }

    @Test
    fun `accepts Indian grouping and symbol`() {
        assertEquals(12_345_678L, Money.parseRupeesToPaise("1,23,456.78"))
        assertEquals(1_500_00L, Money.parseRupeesToPaise("₹1,500"))
    }

    @Test
    fun `rejects garbage, blanks, and over-precision`() {
        assertNull(Money.parseRupeesToPaise(""))
        assertNull(Money.parseRupeesToPaise("abc"))
        assertNull(Money.parseRupeesToPaise("1.999"))
    }

    @Test
    fun `round trips with format`() {
        val paise = Money.parseRupeesToPaise("1,23,456.78")!!
        assertEquals("₹1,23,456.78", Money.format(paise))
    }
}
