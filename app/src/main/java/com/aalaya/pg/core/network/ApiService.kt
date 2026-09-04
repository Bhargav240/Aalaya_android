package com.aalaya.pg.core.network

import com.aalaya.pg.core.network.dto.*
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * The complete backend surface. Base URL is `.../api/v1/`, so paths here are
 * relative. Auth is attached by an OkHttp interceptor, except where noted
 * (provisioning uses a header; discovery is anonymous).
 */
interface ApiService {

    // ── Bootstrap & auth ─────────────────────────────────────────────────────
    @POST("orgs")
    suspend fun provisionOrg(
        @Header("x-provisioning-key") key: String,
        @Body body: ProvisionRequest,
    ): ProvisionResponse

    @POST("auth/email/send-otp")
    suspend fun requestEmailOtp(@Body body: RequestEmailOtpRequest): StatusResponse

    @POST("auth/email/verify-otp")
    suspend fun verifyEmailOtp(@Body body: VerifyEmailOtpRequest): VerifyResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): AccessResponse

    @POST("auth/logout")
    suspend fun logout(): StatusResponse

    @POST("auth/switch-organization")
    suspend fun switchOrganization(@Body body: SwitchOrganizationRequest): TokensResponse

    @POST("auth/verify-identity")
    suspend fun verifyIdentity(@Body body: VerifyIdentityRequest): VerifyIdentityResponse

    // ── Me ───────────────────────────────────────────────────────────────────
    @GET("me")
    suspend fun me(): MeResponse

    @GET("me/memberships")
    suspend fun memberships(): List<MembershipDto>

    @GET("me/tenant-invitations")
    suspend fun myTenantInvitations(): List<TenantInvitationDto>

    // ── Tenant invitations and onboarding ──────────────────────────────────
    @GET("tenant-invitations")
    suspend fun tenantInvitations(): List<TenantInvitationDto>

    @POST("tenant-invitations")
    suspend fun createTenantInvitation(
        @Body body: CreateTenantInvitationRequest,
    ): TenantInvitationDto

    @GET("tenant-invitations/{id}")
    suspend fun tenantInvitation(@Path("id") id: String): TenantInvitationDto

    @POST("tenant-invitations/{id}/accept")
    suspend fun acceptTenantInvitation(@Path("id") id: String): StatusResponse

    @POST("tenant-invitations/{id}/decline")
    suspend fun declineTenantInvitation(@Path("id") id: String): StatusResponse

    @POST("tenant-invitations/{id}/approve")
    suspend fun approveTenantInvitation(@Path("id") id: String): StatusResponse

    @POST("tenant-invitations/{id}/reject")
    suspend fun rejectTenantInvitation(@Path("id") id: String): StatusResponse

    @POST("tenant-invitations/{id}/cancel")
    suspend fun cancelTenantInvitation(@Path("id") id: String): StatusResponse

    @POST("tenant-invitations/{id}/resend")
    suspend fun resendTenantInvitation(@Path("id") id: String): TenantInvitationDto

    @GET("tenant-invitations/{id}/onboarding-status")
    suspend fun tenantOnboardingStatus(@Path("id") id: String): OnboardingStatusDto

    @POST("tenant-invitations/{id}/digilocker/start")
    suspend fun startDigiLocker(@Path("id") id: String): DigiLockerStartDto

    @GET("tenant-invitations/{id}/verification")
    suspend fun invitationVerification(@Path("id") id: String): InvitationVerificationDto?

    @POST("tenant-invitations/{id}/manual-review")
    suspend fun manualKycReview(
        @Path("id") id: String,
        @Body body: ManualKycReviewRequest,
    ): StatusResponse

    @GET("me/receipts")
    suspend fun myReceipts(): List<ReceiptDto>

    @Streaming
    @GET("me/receipts/{id}/download")
    suspend fun downloadMyReceipt(@Path("id") receiptId: String): ResponseBody

    @GET("me/away-periods")
    suspend fun myAwayPeriods(): List<AwayPeriodDto>

    @POST("away-periods")
    suspend fun reportAway(@Body req: ReportAwayRequest): IdResponse

    /** Correct the dates of a period already reported. */
    @PATCH("me/away-periods/{id}")
    suspend fun updateAwayPeriod(
        @Path("id") id: String,
        @Body req: ReportAwayRequest,
    ): StatusResponse

    @DELETE("me/away-periods/{id}")
    suspend fun cancelAwayPeriod(@Path("id") id: String): StatusResponse

    /** Owner: whole org. Manager: their own branch. */
    @GET("away-periods")
    suspend fun awayPeriods(): List<AwayStaffPeriodDto>

    @GET("me/ledger")
    suspend fun myLedger(): LedgerDto

    // ── Branches / rooms / beds ──────────────────────────────────────────────
    @GET("branches")
    suspend fun branches(): List<BranchDto>

    @POST("branches")
    suspend fun createBranch(@Body body: CreateBranchRequest): StatusResponse

    @POST("branches/with-layout")
    suspend fun createBranchWithLayout(@Body body: CreateBranchWithLayoutRequest): CreatedBranchResponse

    @PATCH("branches/{id}")
    suspend fun updateBranch(@Path("id") id: String, @Body body: UpdateBranchRequest): ChangeResponse

    @DELETE("branches/{id}")
    suspend fun deleteBranch(@Path("id") id: String): ChangeResponse

    @GET("branches/{branchId}/floors")
    suspend fun floors(@Path("branchId") branchId: String): List<FloorDto>

    @POST("branches/{branchId}/floors")
    suspend fun createFloor(
        @Path("branchId") branchId: String,
        @Body body: FloorRequest,
    ): ChangeResponse

    @PATCH("floors/{id}")
    suspend fun updateFloor(@Path("id") id: String, @Body body: FloorRequest): ChangeResponse

    @DELETE("floors/{id}")
    suspend fun deleteFloor(@Path("id") id: String): ChangeResponse

    @GET("branches/{branchId}/rooms")
    suspend fun rooms(@Path("branchId") branchId: String): List<RoomDto>

    @POST("branches/{branchId}/rooms")
    suspend fun createRoom(@Path("branchId") branchId: String, @Body body: CreateRoomRequest): MutationResponse

    @PATCH("rooms/{id}")
    suspend fun updateRoom(@Path("id") id: String, @Body body: UpdateRoomRequest): ChangeResponse

    @DELETE("rooms/{id}")
    suspend fun deleteRoom(@Path("id") id: String): ChangeResponse

    @GET("rooms/{roomId}/beds")
    suspend fun beds(@Path("roomId") roomId: String): List<BedDto>

    /** Every bed in a branch (one call) — the payments/occupancy screens walk
     * placements from this single fetch instead of fanning out per room. */
    @GET("branches/{branchId}/beds")
    suspend fun branchBeds(@Path("branchId") branchId: String): List<BedDto>

    @POST("rooms/{roomId}/beds")
    suspend fun createBed(@Path("roomId") roomId: String, @Body body: CreateBedRequest): MutationResponse

    // ── Residents ─────────────────────────────────────────────────────────────
    @GET("branches/{branchId}/residents")
    suspend fun residents(@Path("branchId") branchId: String): List<ResidentDto>

    // A7 batch dues: one call replacing the N ledger fan-out.
    @GET("branches/{branchId}/dues")
    suspend fun branchDues(@Path("branchId") branchId: String): List<ResidentDueDto>

    @POST("branches/{branchId}/residents")
    suspend fun createResident(@Path("branchId") branchId: String, @Body body: CreateResidentRequest): IdResponse

    @PATCH("residents/{id}")
    suspend fun updateResident(@Path("id") id: String, @Body body: UpdateResidentRequest): ChangeResponse

    @DELETE("residents/{id}")
    suspend fun deleteResident(@Path("id") id: String): ChangeResponse

    @POST("residents/{id}/assign-bed")
    suspend fun assignBed(@Path("id") id: String, @Body body: AssignBedRequest): StatusResponse

    @POST("residents/{id}/move-out")
    suspend fun moveOut(@Path("id") id: String, @Body body: MoveOutRequest): SettlementDto

    // ── Payments & receipts ───────────────────────────────────────────────────
    /**
     * Records a payment and issues its invoice. The Idempotency-Key makes a
     * retried POST return the original receipt instead of charging the
     * resident twice and generating a second invoice for the same money.
     */
    @POST("residents/{id}/payments")
    suspend fun recordPayment(
        @Path("id") residentId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: PaymentRequest,
    ): PaymentResponse

    /** Reverses the resident's most recent payment, withdrawing its receipt. */
    @POST("residents/{id}/payments/void")
    suspend fun voidLastPayment(@Path("id") residentId: String): VoidPaymentResponse

    @GET("residents/{id}/receipts")
    suspend fun residentReceipts(@Path("id") residentId: String): List<ReceiptDto>

    @GET("residents/{id}/ledger")
    suspend fun residentLedger(@Path("id") residentId: String): LedgerDto

    @Streaming
    @GET("receipts/{id}/download")
    suspend fun downloadReceipt(@Path("id") receiptId: String): ResponseBody

    // ── Maker-checker ─────────────────────────────────────────────────────────
    @GET("change-requests")
    suspend fun changeRequests(): List<ChangeRequestDto>

    @POST("change-requests/{id}/approve")
    suspend fun approveChange(@Path("id") id: String): StatusResponse

    @POST("change-requests/{id}/reject")
    suspend fun rejectChange(@Path("id") id: String): StatusResponse

    // ── Complaints ────────────────────────────────────────────────────────────
    @GET("complaints")
    suspend fun complaints(): List<ComplaintDto>

    @POST("complaints")
    suspend fun raiseComplaint(@Body body: RaiseComplaintRequest): IdResponse

    @POST("complaints/{id}/transition")
    suspend fun transitionComplaint(@Path("id") id: String, @Body body: TransitionRequest): StatusResponse

    // ── Notices ───────────────────────────────────────────────────────────────
    @GET("notices")
    suspend fun notices(): List<NoticeDto>

    @POST("notices")
    suspend fun createNotice(@Body body: CreateNoticeRequest): IdResponse

    // ── Finance ───────────────────────────────────────────────────────────────
    @GET("finance/branch/{id}")
    suspend fun financeBranch(@Path("id") branchId: String): FinanceReportDto

    @GET("finance/org")
    suspend fun financeOrg(): FinanceOrgDto

    // ── Receipt template & audit & admin ──────────────────────────────────────
    @GET("org/receipt-template")
    suspend fun receiptTemplate(): ReceiptTemplateDto

    @PUT("org/receipt-template")
    suspend fun setReceiptTemplate(@Body body: ReceiptTemplateDto): StatusResponse

    @GET("audit")
    suspend fun audit(@Query("limit") limit: Int? = null): List<AuditEntryDto>

    @POST("admin/archive")
    suspend fun archive(@Body body: ArchiveRequest): kotlinx.serialization.json.JsonElement

    @POST("admin/promote")
    suspend fun promote(): kotlinx.serialization.json.JsonElement

    // ── Public discovery / marketplace ────────────────────────────────────────
    @PUT("listings/{branchId}")
    suspend fun publishListing(@Path("branchId") branchId: String, @Body body: PublishListingRequest): StatusResponse

    @GET("discover/listings")
    suspend fun discoverListings(@Query("limit") limit: Int? = null): List<ListingDto>

    @GET("discover/search")
    suspend fun discoverSearch(
        @Query("q") q: String? = null,
        @Query("city") city: String? = null,
        @Query("limit") limit: Int? = null,
    ): List<ListingDto>

    @GET("discover/nearby")
    suspend fun discoverNearby(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("limit") limit: Int? = null,
    ): List<ListingDto>

    @GET("discover/listings/{branchId}")
    suspend fun discoverOne(@Path("branchId") branchId: String): ListingDto

    // ── Enquiries (manager inbox + authenticated explorer enquire) ────────────
    @GET("enquiries")
    suspend fun enquiries(): List<EnquiryDto>

    @POST("enquiries/{id}/status")
    suspend fun setEnquiryStatus(@Path("id") id: String, @Body body: EnquiryStatusRequest): StatusResponse

    @POST("listings/{branchId}/enquire")
    suspend fun enquire(@Path("branchId") branchId: String, @Body body: EnquireRequest): StatusResponse

    // ── Listing publish workflow (owner/manager + operator queue) ────────────
    @GET("listings")
    suspend fun myListings(): List<ListingDto>

    @POST("listings/{branchId}/submit")
    suspend fun submitListing(@Path("branchId") branchId: String): StatusResponse

    @POST("operator/listings/{branchId}/publish")
    suspend fun operatorPublishListing(@Path("branchId") branchId: String): StatusResponse

    @POST("operator/listings/{branchId}/reject")
    suspend fun operatorRejectListing(@Path("branchId") branchId: String, @Body body: OpsRejectRequest): StatusResponse

    @POST("operator/listings/{branchId}/unlist")
    suspend fun operatorUnlistListing(@Path("branchId") branchId: String): StatusResponse
}
