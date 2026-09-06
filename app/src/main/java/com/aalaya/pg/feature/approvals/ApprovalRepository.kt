package com.aalaya.pg.feature.approvals

import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.network.ApiService
import com.aalaya.pg.core.network.NetworkExecutor
import com.aalaya.pg.core.network.dto.ChangeRequestDto
import com.aalaya.pg.feature.branches.toUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Maker-checker: owners review pending change requests. */
@Singleton
class ApprovalRepository @Inject constructor(
    private val api: ApiService,
    private val executor: NetworkExecutor,
) {
    suspend fun listChanges(status: String? = null): ApiResult<List<ChangeRequestDto>> =
        executor.execute { api.changeRequests(status) }

    suspend fun pending(): ApiResult<List<ChangeRequestDto>> = listChanges("all")

    suspend fun approve(id: String): ApiResult<Unit> =
        executor.execute { api.approveChange(id) }.toUnit()

    suspend fun reject(id: String): ApiResult<Unit> =
        executor.execute { api.rejectChange(id) }.toUnit()
}
