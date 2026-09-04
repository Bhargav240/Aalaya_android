package com.aalaya.pg.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formats backend epoch-seconds timestamps for display. */
object DateFmt {
    private val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

    fun epochSeconds(seconds: Long): String =
        runCatching {
            Instant.ofEpochSecond(seconds).atZone(ZoneId.systemDefault()).format(formatter)
        }.getOrDefault("")
}
