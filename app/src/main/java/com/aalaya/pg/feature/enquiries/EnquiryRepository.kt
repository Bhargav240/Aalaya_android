package com.aalaya.pg.feature.enquiries

import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.network.ApiService
import com.aalaya.pg.core.network.NetworkExecutor
import com.aalaya.pg.core.network.dto.EnquireRequest
import com.aalaya.pg.core.network.dto.EnquiryDto
import com.aalaya.pg.core.network.dto.EnquiryStatusRequest
import com.aalaya.pg.feature.branches.toUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnquiryRepository @Inject constructor(
    private val api: ApiService,
    private val executor: NetworkExecutor,
) {
    /** Manager's inbox of enquiries across their listings. */
    suspend fun list(): ApiResult<List<EnquiryDto>> = executor.execute { api.enquiries() }

    suspend fun setStatus(id: String, status: String): ApiResult<Unit> =
        executor.execute { api.setEnquiryStatus(id, EnquiryStatusRequest(status)) }.toUnit()

    /** Send an enquiry for a listing using the authenticated explorer session. */
    suspend fun enquire(branchId: String, note: String?): ApiResult<Unit> =
        executor.execute {
            api.enquire(
                branchId,
                EnquireRequest(
                    note = note?.trim()?.ifBlank { null },
                ),
            )
        }.toUnit()
}
