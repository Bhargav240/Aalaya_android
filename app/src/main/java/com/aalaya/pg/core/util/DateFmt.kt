package com.aalaya.pg.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formats backend timestamps for display. */
object DateFmt {
    private val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    private val isoFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, hh:mm a", Locale.getDefault())

    fun epochSeconds(seconds: Long): String =
        runCatching {
            Instant.ofEpochSecond(seconds).atZone(ZoneId.systemDefault()).format(formatter)
        }.getOrDefault("")

    fun isoDateTime(isoStr: String?): String =
        if (isoStr.isNullOrBlank()) ""
        else runCatching {
            Instant.parse(isoStr).atZone(ZoneId.systemDefault()).format(isoFormatter)
        }.getOrDefault(isoStr.take(16).replace("T", " "))
}
