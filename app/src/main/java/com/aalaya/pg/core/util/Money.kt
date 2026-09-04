package com.aalaya.pg.core.util

import java.math.BigInteger

/**
 * All money in the backend is integer paise (1 rupee = 100 paise) to avoid
 * floating-point drift. This renders paise as Indian-grouped rupees, e.g.
 * 150000 -> "₹1,500", 12345678 -> "₹1,23,456.78".
 */
object Money {
    private val paisePerRupee = BigInteger.valueOf(100)

    fun format(paise: Long, symbol: String = "₹"): String {
        val absolute = BigInteger.valueOf(paise).abs()
        val (rupees, remainder) = absolute.divideAndRemainder(paisePerRupee)
        val sign = if (paise < 0) "-" else ""
        val fraction = if (remainder.signum() == 0) {
            ""
        } else {
            ".${remainder.toString().padStart(2, '0')}"
        }

        return "$sign$symbol${groupIndian(rupees.toString())}$fraction"
    }

    /**
     * Parses a user-typed rupee amount ("1500", "1,500.50", "1500,50") into
     * paise EXACTLY via BigDecimal — no floating-point drift. Returns null for
     * anything that isn't a clean amount with at most two decimal places.
     */
    fun parseRupeesToPaise(input: String): Long? {
        val cleaned = input.trim().replace(",", "").replace("₹", "")
        if (cleaned.isEmpty()) return null
        return try {
            val value = java.math.BigDecimal(cleaned)
            if (value.scale() > 2) return null
            value.movePointRight(2).longValueExact()
        } catch (_: NumberFormatException) {
            null
        } catch (_: ArithmeticException) {
            null
        }
    }

    private fun groupIndian(digits: String): String {
        if (digits.length <= 3) return digits

        val finalGroup = digits.takeLast(3)
        val leading = digits.dropLast(3)
        val firstGroupLength = if (leading.length % 2 == 0) 2 else 1
        val groups = mutableListOf(leading.take(firstGroupLength))

        var index = firstGroupLength
        while (index < leading.length) {
            groups += leading.substring(index, index + 2)
            index += 2
        }

        return "${groups.joinToString(",")},$finalGroup"
    }
}
