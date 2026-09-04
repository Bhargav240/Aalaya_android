package com.aalaya.pg.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {
    @Test
    fun `whole rupees have no decimals and Indian grouping`() {
        assertEquals("₹1,500", Money.format(150_000))
        assertEquals("₹0", Money.format(0))
        assertEquals("₹1,00,000", Money.format(10_000_000))
    }

    @Test
    fun `paise remainder shows two decimals`() {
        assertEquals("₹1,234.56", Money.format(123_456))
        assertEquals("₹0.99", Money.format(99))
    }

    @Test
    fun `custom symbol is honoured`() {
        assertEquals("Rs.500", Money.format(50_000, symbol = "Rs."))
    }

    @Test
    fun `negative and extreme values remain exact`() {
        assertEquals("-₹1,234.56", Money.format(-123_456))
        assertEquals("-₹92,23,37,20,36,85,47,758.08", Money.format(Long.MIN_VALUE))
    }
}
