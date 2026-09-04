package com.aalaya.pg.feature.tenant

import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.network.ApiService
import com.aalaya.pg.core.network.NetworkExecutor
import com.aalaya.pg.core.network.dto.LedgerDto
import com.aalaya.pg.core.network.dto.ReceiptDto
import javax.inject.Inject
import javax.inject.Singleton

/** The signed-in resident's own view: their ledger and receipts. */
@Singleton
class TenantRepository @Inject constructor(
    private val api: ApiService,
    private val executor: NetworkExecutor,
) {
    @Volatile
    private var cachedLedger: LedgerDto? = null
    @Volatile
    private var cachedReceipts: List<ReceiptDto>? = null

    fun cachedLedger(): LedgerDto? = cachedLedger
    fun cachedReceipts(): List<ReceiptDto>? = cachedReceipts

    suspend fun myLedger(forceRefresh: Boolean = false): ApiResult<LedgerDto> {
        if (!forceRefresh) {
            cachedLedger?.let { return ApiResult.Success(it) }
        }
        return when (val result = executor.execute { api.myLedger() }) {
            is ApiResult.Success -> {
                cachedLedger = result.data
                result
            }
            is ApiResult.Failure -> result
        }
    }
    suspend fun myReceipts(forceRefresh: Boolean = false): ApiResult<List<ReceiptDto>> {
        if (!forceRefresh) cachedReceipts?.let { return ApiResult.Success(it) }
        return executor.execute { api.myReceipts() }.also { result ->
            if (result is ApiResult.Success) cachedReceipts = result.data
        }
    }
}
