package com.aalaya.pg.feature.away

import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.network.ApiService
import com.aalaya.pg.core.network.NetworkExecutor
import com.aalaya.pg.core.network.dto.AwayPeriodDto
import com.aalaya.pg.core.network.dto.ReportAwayRequest
import com.aalaya.pg.feature.branches.toUnit
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** A period the resident told the PG they'd be away. */
data class AwayPeriod(val id: String, val from: LocalDate, val to: LocalDate, val note: String?)

/**
 * Reports away days to the backend.
 *
 * This is a safety notification only: it tells the PG the bed will be empty
 * and does NOT touch rent, dues, or the ledger. (The backend enforces that —
 * `money.rs` is asserted never to reference away days — so this must not
 * promise the tenant a rent reduction.)
 */
@Singleton
class AwayRepository @Inject constructor(
    private val api: ApiService,
    private val executor: NetworkExecutor,
) {
    suspend fun list(): ApiResult<List<AwayPeriod>> = executor.execute {
        api.myAwayPeriods().map(::toModel).sortedByDescending { it.from }
    }

    suspend fun report(from: LocalDate, to: LocalDate, note: String?): ApiResult<Unit> =
        executor.execute { api.reportAway(request(from, to, note)) }.toUnit()

    /** Corrects a period already reported, keeping its id and audit trail. */
    suspend fun update(id: String, from: LocalDate, to: LocalDate, note: String?): ApiResult<Unit> =
        executor.execute { api.updateAwayPeriod(id, request(from, to, note)) }.toUnit()

    suspend fun cancel(id: String): ApiResult<Unit> =
        executor.execute { api.cancelAwayPeriod(id) }.toUnit()
}

private fun request(from: LocalDate, to: LocalDate, note: String?) = ReportAwayRequest(
    fromDate = from.toString(),
    toDate = to.toString(),
    note = note?.trim()?.ifBlank { null },
)

private fun toModel(dto: AwayPeriodDto): AwayPeriod = AwayPeriod(
    id = dto.id,
    from = LocalDate.parse(dto.fromDate),
    to = LocalDate.parse(dto.toDate),
    note = dto.note,
)
