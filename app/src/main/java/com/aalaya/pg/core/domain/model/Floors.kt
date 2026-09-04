package com.aalaya.pg.core.domain.model

/**
 * Floors are not modelled by the backend — `RoomDto` carries only
 * `id/branchId/number`. The design's Branch → Floor → Room cascade is therefore
 * derived from the room-numbering convention the product already uses, the same
 * one the HTML mockup generates rooms with: the leading character of the room
 * number is its floor.
 *
 *   "101" → floor 1      "204" → floor 2      "G01" → ground floor
 *
 * If a room number doesn't follow the convention it falls into [UNKNOWN] rather
 * than being hidden, so nothing silently disappears from the explorer.
 */
object Floors {
    const val UNKNOWN = "?"

    /**
     * Explicit "All floors" choice. The cascade distinguishes *not yet chosen*
     * (null — the next dropdown stays hidden) from *deliberately all* (this),
     * which is what the design's `<option value="all">` does.
     */
    const val ALL = "__all__"

    /** The floor key for a room number, e.g. "101" → "1", "G01" → "G". */
    fun keyOf(roomNumber: String): String {
        val first = roomNumber.trim().firstOrNull() ?: return UNKNOWN
        return when {
            first.isDigit() -> first.toString()
            first.isLetter() -> first.uppercaseChar().toString()
            else -> UNKNOWN
        }
    }

    /** Human label for a floor key, matching the mockup's "Ground floor" / "Floor 1". */
    fun labelOf(key: String): String = when (key) {
        "G" -> "Ground floor"
        UNKNOWN -> "Other rooms"
        else -> if (key.all { it.isDigit() }) "Floor $key" else "Block $key"
    }

    /**
     * Distinct floor keys across [roomNumbers], ground first, then numeric
     * ascending, with anything unrecognised last.
     */
    fun keysOf(roomNumbers: Collection<String>): List<String> =
        roomNumbers.map(::keyOf).distinct().sortedWith(
            compareBy(
                { it == UNKNOWN },
                { it != "G" },
                { it.toIntOrNull() ?: Int.MAX_VALUE },
                { it },
            ),
        )
}
