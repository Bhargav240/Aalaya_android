package com.aalaya.pg.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.Serializable

// Generic envelopes reused across endpoints.

@Serializable
data class IdResponse(val id: String)

@Serializable
data class StatusResponse(val status: String)

/** Maker-checker result: "applied" (done) or "pending" (queued for approval). */
@Serializable
data class ChangeResponse(
    val status: String,
    val changeRequestId: String? = null,
) {
    /** True when the change took effect immediately (owner), rather than queuing. */
    val applied: Boolean get() = status == "applied"
}

/**
 * The result of creating something that a manager may only propose.
 *
 * An owner's create returns the new [id]; a manager's returns
 * `status = "pending"` with no id, because the row does not exist until the
 * owner approves it.
 */
@Serializable
data class MutationResponse(
    val id: String? = null,
    val status: String = "applied",
    val changeRequestId: String? = null,
) {
    val applied: Boolean get() = status == "applied"
}

// ─── Branches / rooms / beds ─────────────────────────────────────────────────

@Serializable
data class BranchDto(val id: String, val name: String)

@Serializable
data class CreateBranchRequest(val name: String)

/** Creates a branch and its initial floor → room → bed hierarchy in one call. */
@Serializable
data class CreateBranchWithLayoutRequest(
    val name: String,
    @SerialName("floor_count") val floorCount: Int,
    @SerialName("rooms_per_floor") val roomsPerFloor: Int,
    @SerialName("beds_per_room") val bedsPerRoom: Int,
)

@Serializable
data class CreatedBranchResponse(
    val status: String,
    val id: String,
    val name: String,
)

@Serializable
data class UpdateBranchRequest(val name: String? = null, val address: String? = null)

/**
 * A floor of a branch.
 *
 * Floors were implied by room numbering long before they were stored, so a
 * room may still have no [RoomDto.floorId] at all; see [Floors] for the
 * fallback that keeps those rooms locatable.
 */
@Serializable
data class FloorDto(
    val id: String,
    val branchId: String,
    val name: String,
    val sortOrder: Int = 0,
    val roomCount: Int = 0,
)

@Serializable
data class FloorRequest(
    val name: String,
    val sortOrder: Int = 0,
    /** Rooms to create with the floor; empty creates a bare floor. */
    val rooms: List<NewRoomRequest> = emptyList(),
)

@Serializable
data class NewRoomRequest(
    val number: String,
    @SerialName("cooling_type") val coolingType: String = "non_ac",
    /** How many people share the room. Beds are labelled A, B, C… from this. */
    val beds: Int = 0,
)

@Serializable
data class RoomDto(
    val id: String,
    val branchId: String,
    val number: String,
    @SerialName("cooling_type") val coolingType: String = "non_ac",
    /** Null for rooms created before floors existed. */
    val floorId: String? = null,
)

@Serializable
data class CreateRoomRequest(
    val number: String,
    @SerialName("cooling_type") val coolingType: String = "non_ac",
    /** The floor the room is being added to, when created from a floor. */
    val floorId: String? = null,
    /** How many people share the room; beds are labelled A, B, C… from this. */
    val beds: Int = 0,
)

@Serializable
data class UpdateRoomRequest(
    val number: String? = null,
    @SerialName("cooling_type") val coolingType: String? = null,
    /**
     * Present-and-null clears the room's floor; absent leaves it unchanged.
     * [JsonNull] is used for the clear case so the key is still emitted.
     */
    val floorId: JsonElement? = null,
)

@Serializable
data class BedDto(
    val id: String,
    val roomId: String,
    val label: String,
    val occupantResidentId: String? = null,
    val occupantName: String? = null,
)

@Serializable
data class CreateBedRequest(val label: String)

@Serializable
data class AssignBedRequest(val bedId: String? = null)

// ─── Residents ───────────────────────────────────────────────────────────────

@Serializable
data class ResidentDto(
    val id: String,
    val branchId: String,
    val fullName: String,
    val status: String,
    val planKind: String,
)

@Serializable
data class CreateResidentRequest(
    val fullName: String,
    val phone: String? = null,
    val planKind: String? = null,
    val rentPaise: Long? = null,
    val depositPaise: Long? = null,
)

@Serializable
data class UpdateResidentRequest(
    val fullName: String? = null,
    val phone: String? = null,
    val planKind: String? = null,
)
