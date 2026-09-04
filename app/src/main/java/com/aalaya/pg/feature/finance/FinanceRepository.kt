package com.aalaya.pg.feature.finance

import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.network.ApiService
import com.aalaya.pg.core.network.NetworkExecutor
import com.aalaya.pg.core.network.dto.FinanceOrgDto
import com.aalaya.pg.core.network.dto.FinanceReportDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceRepository @Inject constructor(
    private val api: ApiService,
    private val executor: NetworkExecutor,
) {
    suspend fun org(): ApiResult<FinanceOrgDto> = executor.execute { api.financeOrg() }
    suspend fun branch(branchId: String): ApiResult<FinanceReportDto> =
        executor.execute { api.financeBranch(branchId) }
}
