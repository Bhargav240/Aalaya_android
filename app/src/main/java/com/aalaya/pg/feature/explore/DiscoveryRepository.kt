package com.aalaya.pg.feature.explore

import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.common.map
import com.aalaya.pg.core.network.ApiService
import com.aalaya.pg.core.network.NetworkExecutor
import com.aalaya.pg.core.network.dto.ListingDto
import com.aalaya.pg.feature.branches.toUnit
import javax.inject.Inject
import javax.inject.Singleton

/** UI-facing listing model, decoupled from the wire DTO. */
data class Listing(
    val branchId: String,
    val title: String,
    val area: String?,
    val city: String?,
    val description: String?,
    val amenities: String?,
    val status: String,
    val bedsAvailable: Int?,
    val startingPricePaise: Long?,
    val distanceKm: Double?,
)

private fun ListingDto.toUi() = Listing(
    branchId = branchId,
    title = title,
    area = area,
    city = city,
    description = description,
    amenities = amenities,
    status = status,
    bedsAvailable = bedsAvailable,
    startingPricePaise = startingPricePaise,
    distanceKm = distanceKm,
)

@Singleton
class DiscoveryRepository @Inject constructor(
    private val api: ApiService,
    private val executor: NetworkExecutor,
) {
    suspend fun list(limit: Int = 50): ApiResult<List<Listing>> =
        executor.execute { api.discoverListings(limit) }.map { it.map(ListingDto::toUi) }

    suspend fun search(query: String?, city: String?): ApiResult<List<Listing>> =
        executor.execute { api.discoverSearch(query, city, 50) }.map { it.map(ListingDto::toUi) }

    suspend fun nearby(lat: Double, lng: Double): ApiResult<List<Listing>> =
        executor.execute { api.discoverNearby(lat, lng, 20) }.map { it.map(ListingDto::toUi) }

    /** The caller's own org listings (with publish status) — owner/manager view. */
    suspend fun myListings(): ApiResult<List<Listing>> =
        executor.execute { api.myListings() }.map { it.map(ListingDto::toUi) }

    /** Send a listing to the operator queue for approval. */
    suspend fun submitListing(branchId: String): ApiResult<Unit> =
        executor.execute { api.submitListing(branchId) }.toUnit()
}
