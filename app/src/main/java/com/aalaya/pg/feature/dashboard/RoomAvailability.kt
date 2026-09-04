package com.aalaya.pg.feature.dashboard

import com.aalaya.pg.core.common.ApiResult
import com.aalaya.pg.core.data.ApiCache
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import com.aalaya.pg.core.domain.model.Floors
import com.aalaya.pg.core.network.dto.BedDto
import com.aalaya.pg.core.network.dto.RoomDto
import com.aalaya.pg.feature.branches.BranchRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * One row of the home screen's room summary: total available beds for a given
 * sharing type right now.
 */
@Serializable
data class RoomSummaryRow(
    val sharingCount: Int,
    val vacantCount: Int,
) {
    val title: String
        get() = when (sharingCount) {
            1 -> "Single Sharing"
            2 -> "Double Sharing"
            3 -> "Triple Sharing"
            else -> "$sharingCount-Sharing"
        }

    val detail: String
        get() = when (sharingCount) {
            1 -> "One bed in each room"
            2 -> "Two beds in each room"
            3 -> "Three beds in each room"
            else -> "$sharingCount beds in each room"
        }

    val availabilityText: String
        get() = if (vacantCount == 1) "1 room available" else "$vacantCount rooms available"
}

/** A single free room, as listed on the room-availability detail screen. */
data class VacantRoomItem(
    val branchId: String,
    val branchName: String,
    val roomId: String,
    val roomNumber: String,
    val vacancyCount: Int,
) {
    val floorKey: String get() = Floors.keyOf(roomNumber)
    val floorLabel: String get() = Floors.labelOf(floorKey)
    val roomLabel: String get() = "Room $roomNumber"
}

/**
 * Which branches the home screen's room figures cover, and what to call that
 * scope on screen.
 */
data class RoomScope(
    val branchIds: List<String>,
    val label: String,
)

/**
 * Vacancy, derived rather than fetched.
 *
 * The backend exposes rooms and beds but no "free rooms" endpoint, so both the
 * summary and the detail list are computed here from one rooms call plus one
 * branch-beds call per branch. Keeping that arithmetic in a single repository
 * stops the home screen and the detail screen from disagreeing about what
 * counts as vacant.
 */
@Singleton
class RoomAvailabilityRepository @Inject constructor(
    private val branches: BranchRepository,
    private val cache: ApiCache,
) {

    suspend fun cachedSummary(branchIds: List<String>): List<RoomSummaryRow>? {
        if (branchIds.isEmpty()) return null
        val key = "room_summary_" + branchIds.sorted().joinToString("_")
        return cache.read(key, ListSerializer(RoomSummaryRow.serializer()), maxAgeMillis = 10 * 60 * 1000L)
    }

    /** Vacant-room counts per sharing type, summed across [branchIds]. */
    suspend fun summary(branchIds: List<String>): ApiResult<List<RoomSummaryRow>> {
        if (branchIds.isEmpty()) return ApiResult.Success(emptyList())

        val totals = mutableMapOf<Int, Int>()
        val branchData = when (val result = loadBranchData(branchIds)) {
            is ApiResult.Success -> result.data
            is ApiResult.Failure -> return result
        }
        for ((_, rooms, beds) in branchData) {
            vacantRoomsBySharing(rooms, beds).forEach { (sharing, count) ->
                totals[sharing] = (totals[sharing] ?: 0) + count
            }
        }

        // Busiest room type first, so the manager sees what they can actually
        // offer before the long tail of odd sharing sizes.
        val rows = totals
            .filterValues { it > 0 }
            .map { (sharing, count) -> RoomSummaryRow(sharing, count) }
            .sortedWith(compareByDescending<RoomSummaryRow> { it.vacantCount }.thenBy { it.sharingCount })
        val key = "room_summary_" + branchIds.sorted().joinToString("_")
        cache.write(key, ListSerializer(RoomSummaryRow.serializer()), rows)
        return ApiResult.Success(rows)
    }

    /** Every free room of one sharing type, across [branchIds]. */
    suspend fun vacantRooms(
        branchIds: List<String>,
        sharingCount: Int,
        branchNames: Map<String, String>,
    ): ApiResult<List<VacantRoomItem>> {
        if (branchIds.isEmpty()) return ApiResult.Success(emptyList())

        val items = mutableListOf<VacantRoomItem>()
        val branchData = when (val result = loadBranchData(branchIds)) {
            is ApiResult.Success -> result.data
            is ApiResult.Failure -> return result
        }
        for ((branchId, rooms, beds) in branchData) {
            val bedsByRoom = beds.groupBy { it.roomId }
            rooms.forEach { room ->
                val roomBeds = bedsByRoom[room.id].orEmpty()
                val vacancies = roomBeds.count { it.occupantResidentId == null }
                if (roomBeds.size == sharingCount && vacancies > 0) {
                    items += VacantRoomItem(
                        branchId = branchId,
                        branchName = branchNames[branchId] ?: "Branch",
                        roomId = room.id,
                        roomNumber = room.number,
                        vacancyCount = vacancies,
                    )
                }
            }
        }

        return ApiResult.Success(
            items.sortedWith(
                compareBy<VacantRoomItem> { it.branchName }
                    .thenBy { it.floorKey }
                    .thenBy { it.roomNumber },
            ),
        )
    }

    /**
     * A branch's rooms and beds are independent, as are separate branches.
     * Run every request at once instead of adding one network round trip per
     * branch to Home and its availability drill-down.
     */
    private data class BranchData(
        val branchId: String,
        val rooms: List<RoomDto>,
        val beds: List<BedDto>,
    )

    private suspend fun loadBranchData(branchIds: List<String>): ApiResult<List<BranchData>> = coroutineScope {
        val results: List<ApiResult<BranchData>> = branchIds.map { branchId ->
            async<ApiResult<BranchData>> {
                val roomsDeferred = async { branches.rooms(branchId) }
                val bedsDeferred = async { branches.branchBeds(branchId) }
                val rooms = roomsDeferred.await()
                val beds = bedsDeferred.await()
                when {
                    rooms is ApiResult.Failure -> rooms
                    beds is ApiResult.Failure -> beds
                    else -> ApiResult.Success(
                        BranchData(
                            branchId = branchId,
                            rooms = (rooms as ApiResult.Success).data,
                            beds = (beds as ApiResult.Success).data,
                        ),
                    )
                }
            }
        }.awaitAll()
        results.filterIsInstance<ApiResult.Failure>().firstOrNull()
            ?: ApiResult.Success(results.map { (it as ApiResult.Success).data })
    }

    /**
     * Available beds, bucketed by room capacity. A room with no beds recorded
     * is not included because it is unfinished.
     */
    private fun vacantRoomsBySharing(rooms: List<RoomDto>, beds: List<BedDto>): Map<Int, Int> {
        val bedsByRoom = beds.groupBy { it.roomId }
        val counts = mutableMapOf<Int, Int>()
        rooms.forEach { room ->
            val roomBeds = bedsByRoom[room.id].orEmpty()
            if (roomBeds.isNotEmpty()) {
                val vacancies = roomBeds.count { it.occupantResidentId == null }
                if (vacancies > 0) {
                    counts[roomBeds.size] = (counts[roomBeds.size] ?: 0) + vacancies
                }
            }
        }
        return counts
    }
}
