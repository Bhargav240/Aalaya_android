package com.aalaya.pg.feature.notices

import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.data.ApiCache
import com.aalaya.pg.core.network.ApiService
import com.aalaya.pg.core.network.NetworkExecutor
import com.aalaya.pg.core.network.dto.CreateNoticeRequest
import com.aalaya.pg.core.network.dto.NoticeDto
import com.aalaya.pg.feature.branches.toUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.builtins.ListSerializer

@Singleton
class NoticeRepository @Inject constructor(
    private val api: ApiService,
    private val executor: NetworkExecutor,
    private val cache: ApiCache,
) {
    @Volatile
    private var cached: List<NoticeDto>? = null

    fun cachedItems(): List<NoticeDto>? = cached

    suspend fun warmCache() {
        cached = cache.read(CACHE_KEY, ListSerializer(NoticeDto.serializer()), MAX_AGE_MS) ?: cached
    }

    suspend fun list(forceRefresh: Boolean = false): ApiResult<List<NoticeDto>> {
        if (!forceRefresh) cached?.let { return ApiResult.Success(it) }
        return executor.execute { api.notices() }.also { result ->
            if (result is ApiResult.Success) {
                cached = result.data
                cache.write(CACHE_KEY, ListSerializer(NoticeDto.serializer()), result.data)
            }
        }
    }

    suspend fun create(title: String, body: String?, branchId: String?): ApiResult<Unit> =
        executor.execute {
            api.createNotice(
                CreateNoticeRequest(
                    title = title.trim(),
                    body = body?.trim()?.ifBlank { null },
                    branchId = branchId,
                ),
            )
        }.toUnit()
}

private const val CACHE_KEY = "notices"
private const val MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000L
