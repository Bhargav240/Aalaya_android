package com.aalaya.pg.feature.branches

import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.common.map
import com.aalaya.pg.core.data.ApiCache
import com.aalaya.pg.core.network.ApiService
import com.aalaya.pg.core.network.NetworkExecutor
import com.aalaya.pg.core.network.dto.BedDto
import com.aalaya.pg.core.network.dto.BranchDto
import com.aalaya.pg.core.network.dto.ChangeResponse
import com.aalaya.pg.core.network.dto.CreateBedRequest
import com.aalaya.pg.core.network.dto.CreateBranchRequest
import com.aalaya.pg.core.network.dto.CreateBranchWithLayoutRequest
import com.aalaya.pg.core.network.dto.CreatedBranchResponse
import com.aalaya.pg.core.network.dto.CreateRoomRequest
import com.aalaya.pg.core.network.dto.FloorDto
import com.aalaya.pg.core.network.dto.FloorRequest
import com.aalaya.pg.core.network.dto.MutationResponse
import com.aalaya.pg.core.network.dto.NewRoomRequest
import com.aalaya.pg.core.network.dto.PublishListingRequest
import com.aalaya.pg.core.network.dto.RoomDto
import com.aalaya.pg.core.network.dto.UpdateBranchRequest
import com.aalaya.pg.core.network.dto.UpdateRoomRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.Serializable

@Singleton
class BranchRepository @Inject constructor(
    private val api: ApiService,
    private val executor: NetworkExecutor,
    private val cache: ApiCache,
) {
    suspend fun branches(): ApiResult<List<BranchDto>> = executor.execute { api.branches() }.also { result ->
        if (result is ApiResult.Success) cache.write(BRANCHES_KEY, BranchList.serializer(), BranchList(result.data))
    }

    suspend fun cachedBranches(): List<BranchDto>? =
        cache.read(BRANCHES_KEY, BranchList.serializer(), BRANCH_MAX_AGE_MS)?.items

    suspend fun cachedOccupancy(branchId: String): OccupancyCache? =
        cache.read(occupancyKey(branchId), OccupancyCache.serializer(), OCCUPANCY_MAX_AGE_MS)

    suspend fun cacheOccupancy(branchId: String, value: OccupancyCache) {
        cache.write(occupancyKey(branchId), OccupancyCache.serializer(), value)
    }

    suspend fun createBranch(name: String): ApiResult<Unit> =
        executor.execute { api.createBranch(CreateBranchRequest(name.trim())) }.let {
            when (it) {
                is ApiResult.Success -> ApiResult.Success(Unit)
                is ApiResult.Failure -> it
            }
        }

    suspend fun createBranchWithLayout(
        name: String,
        floorCount: Int,
        roomsPerFloor: Int,
        bedsPerRoom: Int,
    ): ApiResult<CreatedBranchResponse> = executor.execute {
        api.createBranchWithLayout(
            CreateBranchWithLayoutRequest(
                name = name.trim(),
                floorCount = floorCount,
                roomsPerFloor = roomsPerFloor,
                bedsPerRoom = bedsPerRoom,
            ),
        )
    }

    /** Maker-checker rename/re-address. */
    suspend fun updateBranch(id: String, name: String?, address: String?): ApiResult<ChangeResponse> =
        executor.execute {
            api.updateBranch(
                id,
                UpdateBranchRequest(
                    name = name?.trim()?.ifBlank { null },
                    address = address?.trim()?.ifBlank { null },
                ),
            )
        }

    /** Maker-checker soft delete. */
    suspend fun deleteBranch(id: String): ApiResult<ChangeResponse> =
        executor.execute { api.deleteBranch(id) }

    suspend fun rooms(branchId: String): ApiResult<List<RoomDto>> =
        executor.execute { api.rooms(branchId) }

    // ── Floors ───────────────────────────────────────────────────────────────
    // Floors sit between a branch and its rooms. Only an owner may reshape
    // them; the server enforces that, and the UI hides the controls to match.

    suspend fun floors(branchId: String): ApiResult<List<FloorDto>> =
        executor.execute { api.floors(branchId) }

    /**
     * All three return whether the change applied outright (owner) or queued
     * for the owner's approval (manager), so the screen can say which.
     */
    suspend fun createFloor(
        branchId: String,
        name: String,
        sortOrder: Int,
        rooms: List<NewRoomRequest> = emptyList(),
    ): ApiResult<ChangeResponse> =
        executor.execute { api.createFloor(branchId, FloorRequest(name.trim(), sortOrder, rooms)) }

    suspend fun updateFloor(id: String, name: String, sortOrder: Int): ApiResult<ChangeResponse> =
        executor.execute { api.updateFloor(id, FloorRequest(name.trim(), sortOrder)) }

    suspend fun deleteFloor(id: String): ApiResult<ChangeResponse> =
        executor.execute { api.deleteFloor(id) }

    /**
     * Move a room onto a floor, or off one when [floorId] is null. This rides
     * the same maker-checker path as a rename, so a manager's change queues for
     * the owner exactly as other room edits do.
     */
    suspend fun setRoomFloor(roomId: String, floorId: String?): ApiResult<ChangeResponse> =
        executor.execute {
            api.updateRoom(
                roomId,
                UpdateRoomRequest(
                    floorId = floorId?.let { JsonPrimitive(it) } ?: JsonNull,
                ),
            )
        }

    /** Maker-checker room rename. */
    suspend fun updateRoom(id: String, number: String, coolingType: String): ApiResult<ChangeResponse> =
        executor.execute { api.updateRoom(id, UpdateRoomRequest(number = number.trim(), coolingType = coolingType)) }

    /** Maker-checker room delete. */
    suspend fun deleteRoom(id: String): ApiResult<ChangeResponse> =
        executor.execute { api.deleteRoom(id) }

    suspend fun createRoom(
        branchId: String,
        number: String,
        coolingType: String,
        floorId: String? = null,
        beds: Int = 0,
    ): ApiResult<MutationResponse> =
        executor.execute {
            api.createRoom(branchId, CreateRoomRequest(number.trim(), coolingType, floorId, beds))
        }

    suspend fun beds(roomId: String): ApiResult<List<BedDto>> =
        executor.execute { api.beds(roomId) }

    /** One-call placement map for the payments/occupancy screens. */
    suspend fun branchBeds(branchId: String): ApiResult<List<BedDto>> =
        executor.execute { api.branchBeds(branchId) }

    suspend fun createBed(roomId: String, label: String): ApiResult<MutationResponse> =
        executor.execute { api.createBed(roomId, CreateBedRequest(label.trim())) }

    suspend fun publishListing(branchId: String, req: PublishListingRequest): ApiResult<Unit> =
        executor.execute { api.publishListing(branchId, req) }.toUnit()
}

@Serializable
private data class BranchList(val items: List<BranchDto>)

/** All independent reads needed to paint the Occupancy screen. */
@Serializable
data class OccupancyCache(
    val rooms: List<RoomDto>,
    val beds: List<BedDto>,
    val floors: List<FloorDto>,
)

private const val BRANCHES_KEY = "branches"
// Cached values are always refreshed on screen entry; keeping the last known
// response for a week lets a returning user see the UI immediately even after
// the app was closed overnight.
private const val BRANCH_MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000L
private const val OCCUPANCY_MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000L
private fun occupancyKey(branchId: String) = "occupancy_$branchId"

/** Collapse any success payload we don't need into Unit. */
internal fun <T> ApiResult<T>.toUnit(): ApiResult<Unit> = when (this) {
    is ApiResult.Success -> ApiResult.Success(Unit)
    is ApiResult.Failure -> this
}
