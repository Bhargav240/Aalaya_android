package com.aalaya.pg.core.network.dto

import kotlinx.serialization.Serializable

// Complaints, notices, away days, public marketplace listings and enquiries.

@Serializable
data class AwayPeriodDto(
    val id: String,
    val residentId: String? = null,
    val fromDate: String,
    val toDate: String,
    val note: String? = null,
)

@Serializable
data class ReportAwayRequest(
    val fromDate: String,
    val toDate: String,
    val note: String? = null,
)

/**
 * The staff view of an away period, from `GET /away-periods`.
 *
 * Richer than [AwayPeriodDto] on purpose: an owner or manager needs the person
 * and the bed, not just a period id.
 */
@Serializable
data class AwayStaffPeriodDto(
    val id: String,
    val residentId: String,
    val residentName: String,
    val roomNumber: String? = null,
    val branchId: String,
    val branchName: String,
    val fromDate: String,
    val toDate: String,
    val note: String? = null,
)

@Serializable
data class ComplaintDto(
    val id: String,
    val branchId: String? = null,
    val raisedBy: String,
    val title: String,
    val description: String? = null,
    val state: String,
    val isBusinessLevel: Boolean,
    val createdEpoch: Long = 0,
    val updatedEpoch: Long = 0,
)

@Serializable
data class RaiseComplaintRequest(
    val title: String,
    val description: String? = null,
    val businessLevel: Boolean? = null,
    val branchId: String? = null,
)

@Serializable
data class TransitionRequest(val to: String)

@Serializable
data class NoticeDto(
    val id: String,
    val branchId: String? = null,
    val title: String,
    val body: String? = null,
    val createdEpoch: Long,
)

@Serializable
data class CreateNoticeRequest(
    val title: String,
    val body: String? = null,
    val branchId: String? = null,
)

// ─── Public discovery / marketplace ──────────────────────────────────────────

@Serializable
data class ListingDto(
    val branchId: String,
    val title: String,
    val area: String? = null,
    val city: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val description: String? = null,
    val amenities: String? = null,
    val status: String = "draft",
    val bedsAvailable: Int? = null,
    val startingPricePaise: Long? = null,
    val distanceKm: Double? = null,
)

@Serializable
data class PublishListingRequest(
    val title: String,
    val area: String? = null,
    val city: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val description: String? = null,
    val amenities: String? = null,
)

@Serializable
data class EnquiryDto(
    val id: String,
    val branchId: String,
    val phone: String,
    val note: String? = null,
    val status: String,
    val createdAtEpoch: Long,
    val prospectName: String? = null,
    val email: String? = null,
)

@Serializable
data class EnquiryStatusRequest(val status: String)

@Serializable
data class EnquireRequest(
    val note: String? = null,
    val phone: String? = null,
)

@Serializable
data class OpsRejectRequest(val note: String? = null)
