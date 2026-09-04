package com.aalaya.pg.feature.complaints

import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.data.ApiCache
import com.aalaya.pg.core.domain.model.ComplaintState
import com.aalaya.pg.core.network.ApiService
import com.aalaya.pg.core.network.NetworkExecutor
import com.aalaya.pg.core.network.dto.ComplaintDto
import com.aalaya.pg.core.network.dto.RaiseComplaintRequest
import com.aalaya.pg.core.network.dto.TransitionRequest
import com.aalaya.pg.feature.branches.toUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.builtins.ListSerializer

@Singleton
class ComplaintRepository @Inject constructor(
    private val api: ApiService,
    private val executor: NetworkExecutor,
    private val cache: ApiCache,
) {
    @Volatile
    private var cached: List<ComplaintDto>? = null

    fun cachedItems(): List<ComplaintDto>? = cached

    suspend fun warmCache() {
        cached = cache.read(CACHE_KEY, ListSerializer(ComplaintDto.serializer()), MAX_AGE_MS) ?: cached
    }

    suspend fun list(): ApiResult<List<ComplaintDto>> = executor.execute { api.complaints() }.also { result ->
        if (result is ApiResult.Success) {
            cached = result.data
            cache.write(CACHE_KEY, ListSerializer(ComplaintDto.serializer()), result.data)
        }
    }

    suspend fun raise(title: String, description: String?, businessLevel: Boolean, branchId: String?): ApiResult<Unit> =
        executor.execute {
            api.raiseComplaint(
                RaiseComplaintRequest(
                    title = title.trim(),
                    description = description?.trim()?.ifBlank { null },
                    businessLevel = businessLevel,
                    branchId = branchId,
                ),
            )
        }.toUnit()

    suspend fun transition(id: String, to: ComplaintState): ApiResult<Unit> =
        executor.execute { api.transitionComplaint(id, TransitionRequest(to.wire)) }.toUnit()
}

private const val CACHE_KEY = "complaints"
private const val MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000L
