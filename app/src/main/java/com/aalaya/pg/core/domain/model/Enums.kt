package com.aalaya.pg.core.domain.model

/** Roles, mirrored 1:1 from the backend (serialized as lowercase). */
enum class Role(val wire: String, val label: String) {
    Owner("owner", "Owner"),
    Manager("manager", "Manager"),
    Tenant("tenant", "Resident"),
    Prospect("prospect", "Prospect"),
    PlatformOperator("platform_operator", "Platform Operator");

    /** Staff-side roles see the management surface; tenants see the resident app. */
    val isStaff: Boolean get() = this == Owner || this == Manager
    val canApprove: Boolean get() = this == Owner
    val canManage: Boolean get() = this == Owner || this == Manager

    companion object {
        fun fromWire(s: String?): Role? = entries.firstOrNull { it.wire == s }
    }
}

enum class PlanKind(val wire: String, val label: String) {
    Monthly("monthly", "Monthly"),
    Weekly("weekly", "Weekly"),
    Daily("daily", "Daily");

    companion object {
        fun fromWire(s: String?): PlanKind = entries.firstOrNull { it.wire == s } ?: Monthly
    }
}

/**
 * The life of a complaint. [label] is what a resident sees; [wire] is what the
 * server stores and must not change — "open" is the API's word for the state a
 * complaint is in the moment it is raised.
 */
enum class ComplaintState(val wire: String, val label: String) {
    Open("open", "Raised"),
    Assigned("assigned", "Assigned"),
    InProgress("in_progress", "In progress"),
    Resolved("resolved", "Resolved"),
    Closed("closed", "Closed");

    /** Valid next states — mirrors ComplaintState::can_transition_to on the server. */
    fun allowedTransitions(): List<ComplaintState> = when (this) {
        Open -> listOf(Assigned, InProgress, Closed)
        Assigned -> listOf(InProgress, Closed)
        InProgress -> listOf(Resolved, Closed)
        Resolved -> listOf(Closed, InProgress)
        Closed -> listOf(InProgress)
    }

    companion object {
        fun fromWire(s: String?): ComplaintState = entries.firstOrNull { it.wire == s } ?: Open
    }
}
